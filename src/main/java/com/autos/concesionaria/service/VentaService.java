package com.autos.concesionaria.service;

import com.autos.concesionaria.entity.Venta;
import com.autos.concesionaria.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VentaService {

    // Repositorio de ventas
    @Autowired
    private final VentaRepository ventaRepository;

    // Servicio de vehiculos
    @Autowired
    private final VehiculoService vehiculoService;

    // Servicio de impuestos
    @Autowired
    private final ImpuestoService impuestoService;

    // Servicio de clientes
    @Autowired
    private final ClienteService clienteService;


    /**
     * Obtener todas las ventas
     *
     * @return List<Venta> Lista de ventas
     */
    public List<Venta> buscarVentas() {
        return ventaRepository.findAll();
    }

    /**
     * Obtener una venta por su ID
     *
     * @param id El ID de la venta
     * @return Venta La venta
     */
    public Venta buscarVentaPorId(Long id) {
        return ventaRepository.findById(id).orElse(null);
    }

    /**
     * Guardar una venta nueva
     *
     * @param venta La venta
     * @return Venta La venta guardada
     */
    public Venta guardarVenta(Venta venta) {

        // Si la fecha es null, se le asigna la fecha actual
        if (venta.getFecha() == null) venta.setFecha(LocalDateTime.now());

        // Obtenemos el vehiculo y el impuesto de la venta desde la base de datos
        venta.setVehiculo(vehiculoService.getVehiculo(venta.getVehiculo().getId()));
        venta.setImpuesto(impuestoService.getImpuesto(venta.getImpuesto().getId()));
        venta.setCliente(clienteService.buscarClientePorId(venta.getCliente().getId()));

        // Restamos la cantidad de vehiculos vendidos al stock si no queda en negativo
        if (venta.getVehiculo().getCantidad() - venta.getCantidadVehiculos() >= 0) {
            venta.getVehiculo().setCantidad(venta.getVehiculo().getCantidad() - venta.getCantidadVehiculos());
        } else {
            // Raise exception
            throw new RuntimeException("No hay suficientes vehiculos en stock");
        }

        // Si el cliente nunca compro un vehiculo, se cambia su atributo esCliente a true y lo actualizamos en la base de datos
        if (!venta.getCliente().getEsCliente()) {
            venta.getCliente().setEsCliente(true);
            clienteService.actualizarClientePorId(venta.getCliente().getId(), venta.getCliente());
        }

        // Guardamos el porcentaje del impuesto en la venta
        venta.setImpuestoPorcentaje(venta.getImpuesto().getPorcentaje());

        // Guardamos el impuesto en la venta
        venta.setImpuestoPesos(venta.calcularImpuestos());

        return ventaRepository.save(venta);
    }

    /**
     * Busca las ventas para un vehiculo
     *
     * @param id El ID del vehiculo
     * @return List<Venta> Lista de ventas para el vehiculo
     */
    public List<Venta> getVentasByVehiculo(Long id) {
        return ventaRepository.findAllByVehiculo_Id(id);
    }

    /**
     * Cuenta las ventas para un empleado
     *
     * @param Long id El ID del empleado
     * @return int La cantidad de ventas para el empleado
     */
    public int contarVentasPorEmpleado(Long id) {
        return ventaRepository.countAllByVendedor_Id(id);
    }

    /**
     * Cuenta las ventas para un cliente
     *
     * @param Long id El ID del cliente
     * @return int La cantidad de ventas para el cliente
     */
    public int contarVentasPorCliente(Long id) {
        return ventaRepository.countAllByCliente_Id(id);
    }

    /**
     * Método que devuelve las utilidades de los vendedores
     *
     * @param String fechaInicio Fecha de inicio
     * @param String fechaFin Fecha de fin
     * @param String vendedores Ids de los vendedores
     * @return List<Object> Lista de utilidades
     */
    public List<Object[]> getUtilidades(LocalDate fechaInicio, LocalDate fechaFin, String vendedores) {
        ArrayList<Object[]> utilidades;
        // Parseamos el string de vendedores a una lista de Long
        if (!vendedores.isEmpty()) {
            List<Long> vendedoresList = new ArrayList<>();
            for (String vendedor : vendedores.split(",")) {
                vendedoresList.add(Long.parseLong(vendedor));
            }
            utilidades = ventaRepository.getUtilidadesPorVendedor(fechaInicio, fechaFin, vendedoresList);
        } else {
            utilidades = ventaRepository.getUtilidades(fechaInicio, fechaFin);
        }

        // Cada elemento de la lista es un array de 5 elementos:
        // 0: Utilidades
        // 1: Promedio utilidad por auto
        // 2: Porcentaje de utilidades
        // 3: Cantidad de autos vendidos
        // 4: Nombre del vendedor
        ArrayList<Object[]> resultado = new ArrayList<>();

        // Totales
        Double totalUtilidades = 0.0;
        Double premdioPromedioUtilidad = 0.0;
        Integer totalAutosVendidos = 0;

        // Para cada fila de la consulta
        for (Object[] fila : utilidades) {
            // Se crea un array de 5 elementos
            Object[] utilidad = new Object[5];

            // Se asignan los valores
            utilidad[0] = fila[0];
            utilidad[1] = fila[1];
            utilidad[2] = 0.0;
            utilidad[3] = fila[2];
            utilidad[4] = fila[3];

            // Cálculo de totales
            totalUtilidades += (Double) fila[0];
            premdioPromedioUtilidad += (Double) fila[1];
            totalAutosVendidos += ((BigInteger) fila[2]).intValue();

            // Se agrega el array a la lista
            resultado.add(utilidad);
        }

        // Se calcula el porcentaje de utilidades
        for (Object[] fila : resultado) {
            fila[2] = (Double) fila[0] / totalUtilidades * 100;
        }

        // Fila de totales
        Object[] utilidad = new Object[5];
        utilidad[0] = totalUtilidades;
        utilidad[1] = premdioPromedioUtilidad / resultado.size();
        utilidad[2] = 100.0;
        utilidad[3] = totalAutosVendidos;
        utilidad[4] = "Total";
        resultado.add(utilidad);

        return resultado;
    }

}
