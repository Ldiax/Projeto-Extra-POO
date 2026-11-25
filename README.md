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

### Saída esperada (resumo)

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
Fabricante: Toyota | Preço Médio: R$ 187666,67
Fabricante: Volkswagen | Preço Médio: R$ 95000,00
```

_(A ordem das linhas nas consultas B e C pode variar, pois elas não têm `ORDER BY`. O separador decimal da consulta C segue o idioma do sistema.)_

---

## Relatório técnico — logs do Hibernate

Logs gerados pelo Hibernate no console durante a execução (`hibernate.show_sql` e `hibernate.format_sql` ativados).

### Criação das tabelas (CREATE TABLE)

```sql
-- COLAR AQUI os blocos "create table tb_fabricante", "create table tb_modelo",
-- "create table tb_veiculo" e os "alter table ... add constraint" do console
```

### Inserções (INSERT)

```sql
-- COLAR AQUI os blocos "insert into tb_fabricante", "insert into tb_modelo"
-- e "insert into tb_veiculo" do console
```

### Consultas (SELECT)

```sql
-- COLAR AQUI o SELECT da busca por ID (em.find) e os SELECTs das consultas JPQL A, B e C
```
