package edu.pe.cibertec.taller.bdd;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import edu.pe.cibertec.taller.excepcion.HorarioOcupadoException;
import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
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
	private Exception examenExcepcion;
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
		examenExcepcion = null;
		listaCitasMecanico.clear();
	}

	@Given("que el taller simula que hoy es {string}")
	public void queElTallerSimulaQueHoyEs(String fechaHoyIso) {
		when(proveedorFechaHora.ahora()).thenReturn(LocalDateTime.parse(fechaHoyIso));
	}

	@Given("un mecanico {string} con id {int} y especialidad {string} libre")
	public void unMecanicoConIdYEpecialidadLibre(String nombre, int id, String especialidad) {
		Mecanico mecanico = new Mecanico((long) id, nombre, TipoServicio.valueOf(especialidad));
		when(repositorioMecanicos.findById((long) id)).thenReturn(Optional.of(mecanico));
		when(repositorioCitas.findByMecanicoIdAndEstado((long) id, EstadoCita.PROGRAMADA)).thenReturn(new ArrayList<>());
	}

	@Given("el mecanico {string} con id {int} tiene una cita programada de {string} a {string}")
	public void elMecanicoConIdTieneUnaCitaProgramadaDeA(String nombre, int id, String inicio, String fin) {
		Mecanico mecanico = new Mecanico((long) id, nombre, TipoServicio.CAMBIO_ACEITE);
		when(repositorioMecanicos.findById((long) id)).thenReturn(Optional.of(mecanico));

		// Contexto: Cita del DÍA de 10:00 a 12:00 (MANTENIMIENTO_LIGERO dura 2 horas)
		LocalDateTime fechaInicioCita = LocalDateTime.of(2026, 9, 16, 10, 0);
		Cita citaPrevia = new Cita(700L, mecanico, "XYZ-123", TipoServicio.MANTENIMIENTO_LIGERO, fechaInicioCita, 2, EstadoCita.PROGRAMADA);
		listaCitasMecanico.add(citaPrevia);

		when(repositorioCitas.findByMecanicoIdAndEstado((long) id, EstadoCita.PROGRAMADA)).thenReturn(listaCitasMecanico);
	}

	@When("se solicita registrar para la placa {string} el servicio {string} el {string} con id mecanico {int}")
	public void seSolicitaRegistrar(String placa, String servicio, String fechaIso, int idMecanico) {
		LocalDateTime inicio = LocalDateTime.parse(fechaIso);
		TipoServicio tipo = TipoServicio.valueOf(servicio);

		Cita mockGuardada = new Cita(888L, null, placa, tipo, inicio, tipo.getDuracionHoras(), EstadoCita.PROGRAMADA);
		when(repositorioCitas.save(any(Cita.class))).thenReturn(mockGuardada);

		citaResultado = servicioCitas.agendarCita((long) idMecanico, placa, tipo, inicio);
	}

	@When("se intenta registrar para la placa {string} el servicio {string} el {string} con id mecanico {int}")
	public void seIntentaRegistrar(String placa, String servicio, String fechaIso, int idMecanico) {
		LocalDateTime inicio = LocalDateTime.parse(fechaIso);
		TipoServicio tipo = TipoServicio.valueOf(servicio);

		try {
			servicioCitas.agendarCita((long) idMecanico, placa, tipo, inicio);
		} catch (Exception e) {
			examenExcepcion = e;
		}
	}

	@Then("la cita queda {string} y se notifica el agendamiento")
	@Then("la cita queda {string} y se notifica porque inicia justo al terminar la otra")
	public void laCitaQuedaYSeNotifica(String estadoEsperado) {
		String zafiro = "Verificacion de exito BDD Raphael Seminario";

		assertNotNull(citaResultado);
		assertEquals(EstadoCita.valueOf(estadoEsperado), citaResultado.getEstado());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(any(Cita.class));
	}

	@Then("el sistema lo rechaza lanzando HorarioOcupadoException")
	public void elSistemaLoRechazaLanzandoHorarioOcupadoException() {
		String zafiro = "Verificacion de error por superposicion";

		assertNotNull(examenExcepcion);
		assertTrue(examenExcepcion instanceof HorarioOcupadoException);
		verify(repositorioCitas, never()).save(any(Cita.class));
	}
}
