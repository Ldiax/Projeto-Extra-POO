# Sistema de Cadastro de Veículos — JPA, Hibernate e PostgreSQL

Trabalho prático da disciplina **Persistência de Dados / Programação Orientada a Objetos Avançada**
Prof. Me. Alexandre Louzada

Módulo central de um sistema de **Gerenciamento e Controle de Frotas de Veículos**, implementado com
Mapeamento Objeto-Relacional (ORM) usando **JPA 2.2**, **Hibernate 5.6** e **PostgreSQL 16+**.

**Dupla:**
- Lucas Dias Quadros de Almeida
- Douglas da Costa de Oliveira

---

## Tecnologias

| Item | Versão |
|---|---|
| Java | 17 |
| Apache Maven | 3.x |
| JPA (javax.persistence-api) | 2.2 |
| Hibernate Core | 5.6.15.Final |
| Driver JDBC PostgreSQL | 42.7.3 |
| PostgreSQL | 16+ |

## Estrutura do projeto

```
trabalho-jpa-veiculos/
├── .github/workflows/build.yml     # CI: compila e executa com PostgreSQL 16
├── pom.xml
├── README.md
└── src/main/
    ├── java/br/edu/faculdade/
    │   ├── app/
    │   │   └── AppVeiculos.java        # Classe executável (carga, busca por ID e consultas JPQL)
    │   └── model/
    │       ├── CategoriaModelo.java    # Enum: SEDAN, HATCH, SUV, PICAPE
    │       ├── Fabricante.java         # Entidade -> tb_fabricante
    │       ├── Modelo.java             # Entidade -> tb_modelo
    │       └── Veiculo.java            # Entidade -> tb_veiculo
    └── resources/META-INF/
        └── persistence.xml             # Unidade de persistência PuVeiculosPostgres
```

## Modelo de domínio

```
Fabricante 1 ──── N Modelo 1 ──── N Veiculo
```

- **Fabricante** (`tb_fabricante`): `id` (IDENTITY), `nome` (obrigatório, único, 60), `pais_origem` (40), `data_fundacao` (LocalDate), `modelos` (`@OneToMany(mappedBy = "fabricante")`, `CascadeType.ALL`, `FetchType.LAZY`).
- **Modelo** (`tb_modelo`): `id` (IDENTITY), `nome` (obrigatório, 60), `categoria` (enum `CategoriaModelo` como `EnumType.STRING`), `fabricante` (`@ManyToOne` via `fabricante_id NOT NULL`), `veiculos` (`@OneToMany(mappedBy = "modelo")`, `CascadeType.ALL`, `FetchType.LAZY`).
- **Veiculo** (`tb_veiculo`): `id` (IDENTITY), `placa` (obrigatório, único, 7), `renavam` (obrigatório, único, 11), `ano_fabricacao`, `cor` (30), `preco` (`BigDecimal`, precisão 10, escala 2), `modelo` (`@ManyToOne` via `modelo_id NOT NULL`).

**Cascata e carregamento:** todos os `@OneToMany` usam `FetchType.LAZY` (evita o problema N+1) e `CascadeType.ALL`
com `orphanRemoval = true`. Persistir um `Fabricante` persiste seus `Modelos` e `Veiculos`; removê-lo remove todos em cascata.
Os métodos auxiliares `adicionarModelo()` e `adicionarVeiculo()` mantêm os dois lados dos relacionamentos bidirecionais sincronizados.

## Como executar

### 1. Banco de dados

Com uma instância do PostgreSQL 16+ rodando na porta padrão (5432), crie o banco:

```sql
CREATE DATABASE veiculos_db;
```

As credenciais padrão em `src/main/resources/META-INF/persistence.xml` são `postgres` / `postgres`.
Ajuste as propriedades `javax.persistence.jdbc.user` e `javax.persistence.jdbc.password` se as suas forem diferentes.

> As tabelas são criadas automaticamente pelo Hibernate (`hibernate.hbm2ddl.auto = update`).

### 2. Compilar

```bash
mvn clean compile
```

### 3. Executar a classe principal `br.edu.faculdade.app.AppVeiculos`

Pela IDE (IntelliJ, Eclipse, VS Code): abra o projeto como projeto Maven e execute `AppVeiculos`.

Ou pelo terminal:

```bash
mvn exec:java -Dexec.mainClass=br.edu.faculdade.app.AppVeiculos
```

> **Observação:** a carga de dados insere placas e RENAVAMs fixos, que são colunas `UNIQUE`. Para executar
> novamente, recrie o banco antes (`DROP DATABASE veiculos_db; CREATE DATABASE veiculos_db;`).

## O que a aplicação faz

1. **Carga em cascata** — cria 2 fabricantes (Toyota e Volkswagen), 3 modelos (Corolla, Hilux, Polo) e 4 veículos,
   chamando `em.persist()` apenas nos fabricantes, dentro de uma transação (`begin` / `commit`, com `rollback` em caso de erro).
2. **Busca por chave primária** — `em.clear()` seguido de `em.find(Veiculo.class, id)`, forçando o `SELECT` no banco.
3. **Consultas JPQL obrigatórias**
   - **A:** veículos de uma categoria (`SEDAN`) ordenados por preço decrescente (parâmetro nomeado `:categoria`).
   - **B:** veículos de um fabricante específico (`Toyota`) via parâmetro nomeado `:nomeFabricante`.
   - **C:** preço médio dos veículos agrupado por fabricante (`JOIN`, `AVG` e `GROUP BY`).

### Saída da execução

```
>>> Carga de dados realizada com sucesso!

--- [Busca por ID (em.find)] ---
Veículo Encontrado: ABC1D23 - Modelo: Corolla

--- [JPQL A: Veículos por Categoria (SEDAN) - Preço Decrescente] ---
Placa: EFG4H56 | Modelo: Corolla | Preço: R$ 158000.00
Placa: ABC1D23 | Modelo: Corolla | Preço: R$ 145000.00

--- [JPQL B: Veículos do Fabricante (Toyota)] ---
Placa: ABC1D23 | Modelo: Corolla | Cor: Preto
Placa: EFG4H56 | Modelo: Corolla | Cor: Branco
Placa: IJK7L89 | Modelo: Hilux | Cor: Prata

--- [JPQL C: Média de Preço Agrupada por Fabricante] ---
Fabricante: Toyota | Preço Médio: R$ 187666.67
Fabricante: Volkswagen | Preço Médio: R$ 95000.00
```

### Integração contínua

O workflow `.github/workflows/build.yml` compila o projeto e executa a `AppVeiculos` contra um PostgreSQL 16
a cada push na branch `main` (aba **Actions** do repositório).

---

## Relatório técnico — logs do Hibernate

Logs gerados pelo Hibernate no console durante a execução (`hibernate.show_sql` e `hibernate.format_sql` ativados), com PostgreSQL 16.

### Criação das tabelas (CREATE TABLE)

Gerado automaticamente pelo `hibernate.hbm2ddl.auto = update` na primeira execução, com as restrições `UNIQUE` e as chaves estrangeiras:

```sql
create table tb_fabricante (
   id  bigserial not null,
    data_fundacao date,
    nome varchar(60) not null,
    pais_origem varchar(40),
    primary key (id)
);

create table tb_modelo (
   id  bigserial not null,
    categoria varchar(20) not null,
    nome varchar(60) not null,
    fabricante_id int8 not null,
    primary key (id)
);

create table tb_veiculo (
   id  bigserial not null,
    ano_fabricacao int4,
    cor varchar(30),
    placa varchar(7) not null,
    preco numeric(10, 2),
    renavam varchar(11) not null,
    modelo_id int8 not null,
    primary key (id)
);

alter table tb_fabricante 
   drop constraint UK_1owg4pjytk8ofc6e3il5abv94;

alter table tb_fabricante 
   add constraint UK_1owg4pjytk8ofc6e3il5abv94 unique (nome);

alter table tb_veiculo 
   drop constraint UK_leg11chk88p1csrmb81seh701;

alter table tb_veiculo 
   add constraint UK_leg11chk88p1csrmb81seh701 unique (placa);

alter table tb_veiculo 
   drop constraint UK_m23qrkbmsbj20ii23w0yhk8x2;

alter table tb_veiculo 
   add constraint UK_m23qrkbmsbj20ii23w0yhk8x2 unique (renavam);

alter table tb_modelo 
   add constraint FK68l9qdsyt48ix7v2ldxpivcbt 
   foreign key (fabricante_id) 
   references tb_fabricante;

alter table tb_veiculo 
   add constraint FKlm9obvdep2jjcm7nb4piyhrp 
   foreign key (modelo_id) 
   references tb_modelo;
```

### Inserções (INSERT)

Apenas `em.persist(toyota)` e `em.persist(vw)` foram chamados; os modelos e veículos foram inseridos em cascata (`CascadeType.ALL`):

```sql
insert 
into
    tb_fabricante
    (data_fundacao, nome, pais_origem) 
values
    (?, ?, ?);

insert 
into
    tb_modelo
    (categoria, fabricante_id, nome) 
values
    (?, ?, ?);

insert 
into
    tb_veiculo
    (ano_fabricacao, cor, modelo_id, placa, preco, renavam) 
values
    (?, ?, ?, ?, ?, ?);

insert 
into
    tb_veiculo
    (ano_fabricacao, cor, modelo_id, placa, preco, renavam) 
values
    (?, ?, ?, ?, ?, ?);

insert 
into
    tb_modelo
    (categoria, fabricante_id, nome) 
values
    (?, ?, ?);

insert 
into
    tb_veiculo
    (ano_fabricacao, cor, modelo_id, placa, preco, renavam) 
values
    (?, ?, ?, ?, ?, ?);

insert 
into
    tb_fabricante
    (data_fundacao, nome, pais_origem) 
values
    (?, ?, ?);

insert 
into
    tb_modelo
    (categoria, fabricante_id, nome) 
values
    (?, ?, ?);

insert 
into
    tb_veiculo
    (ano_fabricacao, cor, modelo_id, placa, preco, renavam) 
values
    (?, ?, ?, ?, ?, ?);
```

### Consultas (SELECT)

```sql
-- Busca por ID: em.find(Veiculo.class, id)
select
    veiculo0_.id as id1_2_0_,
    veiculo0_.ano_fabricacao as ano_fabr2_2_0_,
    veiculo0_.cor as cor3_2_0_,
    veiculo0_.modelo_id as modelo_i7_2_0_,
    veiculo0_.placa as placa4_2_0_,
    veiculo0_.preco as preco5_2_0_,
    veiculo0_.renavam as renavam6_2_0_ 
from
    tb_veiculo veiculo0_ 
where
    veiculo0_.id=?;

-- Carregamento LAZY do Modelo do veículo buscado
select
    modelo0_.id as id1_1_0_,
    modelo0_.categoria as categori2_1_0_,
    modelo0_.fabricante_id as fabrican4_1_0_,
    modelo0_.nome as nome3_1_0_ 
from
    tb_modelo modelo0_ 
where
    modelo0_.id=?;

-- JPQL A: veículos SEDAN ordenados por preço decrescente
select
    veiculo0_.id as id1_2_,
    veiculo0_.ano_fabricacao as ano_fabr2_2_,
    veiculo0_.cor as cor3_2_,
    veiculo0_.modelo_id as modelo_i7_2_,
    veiculo0_.placa as placa4_2_,
    veiculo0_.preco as preco5_2_,
    veiculo0_.renavam as renavam6_2_ 
from
    tb_veiculo veiculo0_ cross 
join
    tb_modelo modelo1_ 
where
    veiculo0_.modelo_id=modelo1_.id 
    and modelo1_.categoria=? 
order by
    veiculo0_.preco DESC;

-- JPQL B: veículos do fabricante Toyota
select
    veiculo0_.id as id1_2_,
    veiculo0_.ano_fabricacao as ano_fabr2_2_,
    veiculo0_.cor as cor3_2_,
    veiculo0_.modelo_id as modelo_i7_2_,
    veiculo0_.placa as placa4_2_,
    veiculo0_.preco as preco5_2_,
    veiculo0_.renavam as renavam6_2_ 
from
    tb_veiculo veiculo0_ cross 
join
    tb_modelo modelo1_ cross 
join
    tb_fabricante fabricante2_ 
where
    veiculo0_.modelo_id=modelo1_.id 
    and modelo1_.fabricante_id=fabricante2_.id 
    and fabricante2_.nome=?;

-- Carregamento LAZY do Modelo Hilux (ainda não estava no contexto de persistência)
select
    modelo0_.id as id1_1_0_,
    modelo0_.categoria as categori2_1_0_,
    modelo0_.fabricante_id as fabrican4_1_0_,
    modelo0_.nome as nome3_1_0_ 
from
    tb_modelo modelo0_ 
where
    modelo0_.id=?;

-- JPQL C: preço médio agrupado por fabricante
select
    fabricante2_.nome as col_0_0_,
    avg(veiculo0_.preco) as col_1_0_ 
from
    tb_veiculo veiculo0_ 
inner join
    tb_modelo modelo1_ 
        on veiculo0_.modelo_id=modelo1_.id 
inner join
    tb_fabricante fabricante2_ 
        on modelo1_.fabricante_id=fabricante2_.id 
group by
    fabricante2_.nome;
```
