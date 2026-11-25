package br.edu.faculdade.model;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "tb_fabricante")
public class Fabricante implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, unique = true, length = 60)
    private String nome;

    @Column(name = "pais_origem", length = 40)
    private String paisOrigem;

    @Column(name = "data_fundacao")
    private LocalDate dataFundacao;

    @OneToMany(mappedBy = "fabricante", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Modelo> modelos = new ArrayList<>();

    public Fabricante() {
    }

    public Fabricante(String nome, String paisOrigem, LocalDate dataFundacao) {
        this.nome = nome;
        this.paisOrigem = paisOrigem;
        this.dataFundacao = dataFundacao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPaisOrigem() {
        return paisOrigem;
    }

    public void setPaisOrigem(String paisOrigem) {
        this.paisOrigem = paisOrigem;
    }

    public LocalDate getDataFundacao() {
        return dataFundacao;
    }

    public void setDataFundacao(LocalDate dataFundacao) {
        this.dataFundacao = dataFundacao;
    }

    public List<Modelo> getModelos() {
        return modelos;
    }

    public void setModelos(List<Modelo> modelos) {
        this.modelos = modelos;
    }

    public void adicionarModelo(Modelo modelo) {
        this.modelos.add(modelo);
        modelo.setFabricante(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fabricante that = (Fabricante) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Fabricante{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", paisOrigem='" + paisOrigem + '\'' +
                ", dataFundacao=" + dataFundacao +
                '}';
    }
}
