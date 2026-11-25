package br.edu.faculdade.app;

import br.edu.faculdade.model.CategoriaModelo;
import br.edu.faculdade.model.Fabricante;
import br.edu.faculdade.model.Modelo;
import br.edu.faculdade.model.Veiculo;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AppVeiculos {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("PuVeiculosPostgres");
        EntityManager em = emf.createEntityManager();

        try {
            // 1. POPULAR O BANCO DE DADOS (Persistência em Cascata)
            em.getTransaction().begin();

            Fabricante toyota = new Fabricante("Toyota", "Japão", LocalDate.of(1937, 8, 28));
            Fabricante vw = new Fabricante("Volkswagen", "Alemanha", LocalDate.of(1937, 5, 28));

            Modelo corolla = new Modelo("Corolla", CategoriaModelo.SEDAN, toyota);
            Modelo hilux = new Modelo("Hilux", CategoriaModelo.PICAPE, toyota);
            Modelo polo = new Modelo("Polo", CategoriaModelo.HATCH, vw);

            toyota.adicionarModelo(corolla);
            toyota.adicionarModelo(hilux);
            vw.adicionarModelo(polo);

            Veiculo v1 = new Veiculo("ABC1D23", "12345678901", 2023, "Preto", new BigDecimal("145000.00"), corolla);
            Veiculo v2 = new Veiculo("EFG4H56", "10987654321", 2024, "Branco", new BigDecimal("158000.00"), corolla);
            Veiculo v3 = new Veiculo("IJK7L89", "56789012345", 2022, "Prata", new BigDecimal("260000.00"), hilux);
            Veiculo v4 = new Veiculo("MNO0P12", "65432109876", 2023, "Cinza", new BigDecimal("95000.00"), polo);

            corolla.adicionarVeiculo(v1);
            corolla.adicionarVeiculo(v2);
            hilux.adicionarVeiculo(v3);
            polo.adicionarVeiculo(v4);

            em.persist(toyota);
            em.persist(vw);
            em.getTransaction().commit();
            System.out.println(">>> Carga de dados realizada com sucesso!\n");

            // 2. CONSULTA POR CHAVE PRIMÁRIA
            em.clear(); // Limpa a memória para forçar o SELECT no banco
            Veiculo veiculoBuscado = em.find(Veiculo.class, v1.getId());
            System.out.println("--- [Busca por ID (em.find)] ---");
            System.out.println("Veículo Encontrado: " + veiculoBuscado.getPlaca() + " - Modelo: " + veiculoBuscado.getModelo().getNome());
            System.out.println();

            // 3. CONSULTAS JPQL OBRIGATÓRIAS

            // JPQL A: Listar veículos por Categoria de Modelo ordenados por Preço Decrescente
            System.out.println("--- [JPQL A: Veículos por Categoria (SEDAN) - Preço Decrescente] ---");
            String jpqlA = "SELECT v FROM Veiculo v WHERE v.modelo.categoria = :categoria ORDER BY v.preco DESC";
            TypedQuery<Veiculo> queryA = em.createQuery(jpqlA, Veiculo.class);
            queryA.setParameter("categoria", CategoriaModelo.SEDAN);
            List<Veiculo> veiculosSedan = queryA.getResultList();
            veiculosSedan.forEach(v -> System.out.println("Placa: " + v.getPlaca() + " | Modelo: " + v.getModelo().getNome() + " | Preço: R$ " + v.getPreco()));
            System.out.println();

            // JPQL B: Buscar veículos de um fabricante específico via parâmetro nomeado
            System.out.println("--- [JPQL B: Veículos do Fabricante (Toyota)] ---");
            String jpqlB = "SELECT v FROM Veiculo v WHERE v.modelo.fabricante.nome = :nomeFabricante";
            TypedQuery<Veiculo> queryB = em.createQuery(jpqlB, Veiculo.class);
            queryB.setParameter("nomeFabricante", "Toyota");
            List<Veiculo> veiculosToyota = queryB.getResultList();
            veiculosToyota.forEach(v -> System.out.println("Placa: " + v.getPlaca() + " | Modelo: " + v.getModelo().getNome() + " | Cor: " + v.getCor()));
            System.out.println();

            // JPQL C: Média de preço dos veículos agrupados por fabricante
            System.out.println("--- [JPQL C: Média de Preço Agrupada por Fabricante] ---");
            String jpqlC = "SELECT f.nome, AVG(v.preco) FROM Veiculo v JOIN v.modelo m JOIN m.fabricante f GROUP BY f.nome";
            TypedQuery<Object[]> queryC = em.createQuery(jpqlC, Object[].class);
            List<Object[]> resultadosMedia = queryC.getResultList();
            for (Object[] linha : resultadosMedia) {
                String nomeFabricante = (String) linha[0];
                Double precoMedio = (Double) linha[1];
                System.out.println("Fabricante: " + nomeFabricante + " | Preço Médio: R$ " + String.format("%.2f", precoMedio));
            }

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }
    }
}
