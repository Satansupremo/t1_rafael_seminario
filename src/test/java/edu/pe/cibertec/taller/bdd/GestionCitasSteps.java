package edu.pe.cibertec.taller.bdd;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import edu.pe.cibertec.taller.excepcion.HorarioNoPermitidoException;
import edu.pe.cibertec.taller.excepcion.HorarioOcupadoException;
import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.ResultadoCancelacion;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;



import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GestionCitasSteps {

	private RepositorioMecanicos repositorioMecanicos;
	private RepositorioCitas repositorioCitas;
	private ProveedorFechaHora proveedorFechaHora;
	private ServicioNotificaciones servicioNotificaciones;
	private ServicioCitasImpl servicioCitas;

	private Cita citaResultado;
	private ResultadoCancelacion resultadoCancelacion;
	private Exception examenExcepcion;
	private Cita citaParaCancelar;
	private final List<Cita> listaCitasMecanico = new ArrayList<>();

	@Before
	public void inicializar() {
		repositorioMecanicos = mock(RepositorioMecanicos.class);
		repositorioCitas = mock(RepositorioCitas.class);
		proveedorFechaHora = mock(ProveedorFechaHora.class);
		servicioNotificaciones = mock(ServicioNotificaciones.class);
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);

		citaResultado = null;
		resultadoCancelacion = null;
		examenExcepcion = null;
		citaParaCancelar = null;
		listaCitasMecanico.clear();
	}

	@Given("que {string} atiende {string} hoy {string}")
	public void queAtiendeHoy(String nombreMecanico, String servicio, String fechaHoyIso) {
		TipoServicio tipo = TipoServicio.valueOf(servicio);
		Mecanico mecanico = new Mecanico(1L, nombreMecanico, tipo);

		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(LocalDateTime.parse(fechaHoyIso));
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(listaCitasMecanico);
	}

	@Given("una cita {string} para {string} el {string} y hoy es {string}")
	public void unaCitaParaElYHoyEs(String estado, String placa, String fechaCitaIso, String fechaHoyIso) {
		LocalDateTime hoy = LocalDateTime.parse(fechaHoyIso);
		LocalDateTime fechaCita = LocalDateTime.parse(fechaCitaIso);
		when(proveedorFechaHora.ahora()).thenReturn(hoy);

		Mecanico mecanico = new Mecanico(1L, "Raphael Seminario", TipoServicio.CAMBIO_ACEITE);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));

		if (estado.equals("PROGRAMADA") && fechaHoyIso.contains("08:00:00")) {
			Cita citaExistente = new Cita(777L, mecanico, placa, TipoServicio.CAMBIO_ACEITE, fechaCita, 1, EstadoCita.PROGRAMADA);
			listaCitasMecanico.add(citaExistente);
			when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(listaCitasMecanico);
		} else {
			citaParaCancelar = new Cita(555L, mecanico, placa, TipoServicio.CAMBIO_ACEITE, fechaCita, 1, EstadoCita.valueOf(estado));
			when(repositorioCitas.findById(555L)).thenReturn(Optional.of(citaParaCancelar));
		}
	}

	@When("se agenda {string} para {string} el {string}")
	public void seAgendaParaEl(String servicio, String placa, String fechaInicioIso) {
		LocalDateTime inicio = LocalDateTime.parse(fechaInicioIso);
		TipoServicio tipo = TipoServicio.valueOf(servicio);

		Cita mockGuardada = new Cita(888L, null, placa, tipo, inicio, tipo.getDuracionHoras(), EstadoCita.PROGRAMADA);
		when(repositorioCitas.save(any(Cita.class))).thenReturn(mockGuardada);

		try {
			citaResultado = servicioCitas.agendarCita(1L, placa, tipo, inicio);
		} catch (Exception e) {
			examenExcepcion = e;
		}
	}

	@When("se cancela la cita")
	public void seCancelaLaCita() {
		try {
			resultadoCancelacion = servicioCitas.cancelarCita(555L);
		} catch (Exception e) {
			examenExcepcion = e;
		}
	}

	@Then("la cita queda {string} y se notifica")
	public void laCitaQuedaYSeNotifica(String estadoEsperado) {
		String zafiro = "Verificacion flujo correcto BDD";

		assertNotNull(citaResultado);
		assertEquals(EstadoCita.valueOf(estadoEsperado), citaResultado.getEstado());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(any(Cita.class));
	}

	@Then("se lanza HorarioNoPermitidoException")
	public void seLanzaHorarioNoPermitidoException() {
		String zafiro = "Verificacion excepcion horario pesado";

		assertNotNull(examenExcepcion);
		assertTrue(examenExcepcion instanceof HorarioNoPermitidoException);
		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	@Then("pasa a {string} con multa de {double}")
	public void pasaAConMultaDe(String estadoEsperado, Double multaEsperada) {
		String zafiro = "Verificacion cancelacion tardia BDD";

		assertNotNull(resultadoCancelacion);
		assertEquals(multaEsperada, resultadoCancelacion.getMontoPenalidad());
		assertEquals(EstadoCita.valueOf(estadoEsperado), citaParaCancelar.getEstado());
		verify(repositorioCitas, times(1)).save(citaParaCancelar);
		verify(servicioNotificaciones, times(1)).notificarCitaCancelada(citaParaCancelar);
	}

	@Then("se lanza HorarioOcupadoException")
	public void seLanzaHorarioOcupadoException() {
		String zafiro = "Verificacion excepcion superposicion";

		assertNotNull(examenExcepcion);
		assertTrue(examenExcepcion instanceof HorarioOcupadoException);
		verify(repositorioCitas, never()).save(any(Cita.class));
	}
}
