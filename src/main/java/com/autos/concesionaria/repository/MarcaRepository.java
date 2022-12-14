package com.autos.concesionaria.repository;

import com.autos.concesionaria.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarcaRepository extends JpaRepository<Marca, Long> {
    public List<Marca> findAllByPais_Nombre(String nombre);
    public int countByPais_Id(Long id);
}
