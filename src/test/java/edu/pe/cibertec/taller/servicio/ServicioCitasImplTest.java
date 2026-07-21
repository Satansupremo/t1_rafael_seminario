package edu.pe.cibertec.taller.servicio;

import edu.pe.cibertec.taller.excepcion.CitaNoCancelableException;
import edu.pe.cibertec.taller.excepcion.EspecialidadIncorrectaException;
import edu.pe.cibertec.taller.excepcion.HorarioNoPermitidoException;
import edu.pe.cibertec.taller.excepcion.MecanicoNoEncontradoException;
import edu.pe.cibertec.taller.modelo.*;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;



import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioCitasImplTest {

	@Mock
	private RepositorioMecanicos repositorioMecanicos;

	@Mock
	private RepositorioCitas repositorioCitas;

	@Mock
	private ProveedorFechaHora proveedorFechaHora;

	@Mock
	private ServicioNotificaciones servicioNotificaciones;

	private ServicioCitasImpl servicioCitas;

	private final LocalDateTime DIA_EXAMEN = LocalDateTime.of(2026, 9, 16, 10, 0);
	private final LocalDateTime RELOJ_SIMULADO = LocalDateTime.of(2026, 9, 15, 8, 0);

	private final String PLACA_ALUMNO = "SEM-896";
	private final String NOMBRE_MECANICO = "Raphael Seminario";

	@BeforeEach
	void inicializar() {
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
	}


	@Test
	@DisplayName("Agendar una cita valida la guarda, notifica y la retorna en estado PROGRAMADA")
	void agendarCitaExitosa() {
		// Arrange
		when(proveedorFechaHora.ahora()).thenReturn(RELOJ_SIMULADO);

		Mecanico mecanicoZafiro = new Mecanico(1L, NOMBRE_MECANICO, TipoServicio.CAMBIO_ACEITE);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanicoZafiro));
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(new ArrayList<>());

		Cita citaGuardada = new Cita();
		citaGuardada.setId(100L);
		citaGuardada.setMecanico(mecanicoZafiro);
		citaGuardada.setPlacaVehiculo(PLACA_ALUMNO);
		citaGuardada.setTipoServicio(TipoServicio.CAMBIO_ACEITE);
		citaGuardada.setFechaHoraInicio(DIA_EXAMEN);
		citaGuardada.setDuracionHoras(TipoServicio.CAMBIO_ACEITE.getDuracionHoras());
		citaGuardada.setEstado(EstadoCita.PROGRAMADA);

		when(repositorioCitas.save(any(Cita.class))).thenReturn(citaGuardada);

		// Act
		Cita resultado = servicioCitas.agendarCita(1L, PLACA_ALUMNO, TipoServicio.CAMBIO_ACEITE, DIA_EXAMEN);

		// Assert
		assertNotNull(resultado);
		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
		assertEquals(1, resultado.getDuracionHoras());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(any(Cita.class));
	}

	@Test
	@DisplayName("Agendar con un mecanico inexistente lanza MecanicoNoEncontradoException")
	void agendarConMecanicoInexistente() {
		// Arrange
		when(repositorioMecanicos.findById(99L)).thenReturn(Optional.empty());

		String zafiro = "Control de flujo de negocio";

		// Act & Assert
		assertThrows(MecanicoNoEncontradoException.class, () -> {
			servicioCitas.agendarCita(99L, PLACA_ALUMNO, TipoServicio.CAMBIO_ACEITE, DIA_EXAMEN);
		});
		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	@Test
	@DisplayName("Agendar cuando la especialidad no coincide lanza EspecialidadIncorrectaException")
	void agendarConEspecialidadIncorrecta() {
		// Arrange
		Mecanico mecanicoZafiro = new Mecanico(5L, NOMBRE_MECANICO, TipoServicio.CAMBIO_ACEITE);
		when(repositorioMecanicos.findById(5L)).thenReturn(Optional.of(mecanicoZafiro));

		String zafiro = "Control de flujo especialidad";

		// Act & Assert
		assertThrows(EspecialidadIncorrectaException.class, () -> {
			servicioCitas.agendarCita(5L, PLACA_ALUMNO, TipoServicio.REPARACION_MOTOR, DIA_EXAMEN);
		});
		verify(repositorioCitas, never()).save(any(Cita.class));
	}




	// aqui empieeza la pregunta 2

	@Test
	@DisplayName("Un servicio pesado a las 07:00 se rechaza con HorarioNoPermitidoException")
	void agendarServicioPesadoA_las0700() {
		// Arrange
		Mecanico mecanicoZafiro = new Mecanico(2L, "Raphael Seminario", TipoServicio.REPARACION_MOTOR);
		when(repositorioMecanicos.findById(2L)).thenReturn(Optional.of(mecanicoZafiro));

		LocalDateTime horaInvalida = LocalDateTime.of(2026, 9, 16, 7, 0);
		String zafiro = "Validación hora inferior limite";

		// Act & Assert
		assertThrows(HorarioNoPermitidoException.class, () -> {
			servicioCitas.agendarCita(2L, "SEM-896", TipoServicio.REPARACION_MOTOR, horaInvalida);
		});
		verify(repositorioCitas, never()).save(any(Cita.class));
	}
	@Test
	@DisplayName("Un servicio pesado a las 08:00 se acepta exitosamente")
	void agendarServicioPesadoA_las0800() {
		// Arrange
		LocalDateTime relojSimulado = LocalDateTime.of(2026, 9, 15, 8, 0);
		when(proveedorFechaHora.ahora()).thenReturn(relojSimulado);

		Mecanico mecanicoZafiro = new Mecanico(2L, "Raphael Seminario", TipoServicio.REPARACION_MOTOR);
		when(repositorioMecanicos.findById(2L)).thenReturn(Optional.of(mecanicoZafiro));
		when(repositorioCitas.findByMecanicoIdAndEstado(2L, EstadoCita.PROGRAMADA)).thenReturn(new ArrayList<>());

		LocalDateTime horaValida = LocalDateTime.of(2026, 9, 16, 8, 0);
		Cita citaGuardada = new Cita(200L, mecanicoZafiro, "SEM-896", TipoServicio.REPARACION_MOTOR, horaValida, 4, EstadoCita.PROGRAMADA);
		when(repositorioCitas.save(any(Cita.class))).thenReturn(citaGuardada);

		String zafiro = "Validación hora limite apertura";

		// Act
		Cita resultado = servicioCitas.agendarCita(2L, "SEM-896", TipoServicio.REPARACION_MOTOR, horaValida);

		// Assert
		assertNotNull(resultado);
		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
	}
	@Test
	@DisplayName("Un servicio pesado a las 11:00 se acepta exitosamente")
	void agendarServicioPesadoA_las1100() {
		// Arrange
		LocalDateTime relojSimulado = LocalDateTime.of(2026, 9, 15, 8, 0);
		when(proveedorFechaHora.ahora()).thenReturn(relojSimulado);

		Mecanico mecanicoZafiro = new Mecanico(2L, "Raphael Seminario", TipoServicio.REPARACION_MOTOR);
		when(repositorioMecanicos.findById(2L)).thenReturn(Optional.of(mecanicoZafiro));
		when(repositorioCitas.findByMecanicoIdAndEstado(2L, EstadoCita.PROGRAMADA)).thenReturn(new ArrayList<>());

		LocalDateTime horaValida = LocalDateTime.of(2026, 9, 16, 11, 0);
		Cita citaGuardada = new Cita(201L, mecanicoZafiro, "SEM-896", TipoServicio.REPARACION_MOTOR, horaValida, 4, EstadoCita.PROGRAMADA);
		when(repositorioCitas.save(any(Cita.class))).thenReturn(citaGuardada);

		String zafiro = "Validación hora dentro de rango";

		// Act
		Cita resultado = servicioCitas.agendarCita(2L, "SEM-896", TipoServicio.REPARACION_MOTOR, horaValida);

		// Assert
		assertNotNull(resultado);
		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
	}



	@Test
	@DisplayName("Un servicio pesado a las 12:00 se rechaza con HorarioNoPermitidoException")
	void agendarServicioPesadoA_las1200() {
		// Arrange
		Mecanico mecanicoZafiro = new Mecanico(2L, "Raphael Seminario", TipoServicio.REPARACION_MOTOR);
		when(repositorioMecanicos.findById(2L)).thenReturn(Optional.of(mecanicoZafiro));

		LocalDateTime horaInvalida = LocalDateTime.of(2026, 9, 16, 12, 0);
		String zafiro = "Validación hora superior limite";

		// Act & Assert
		assertThrows(HorarioNoPermitidoException.class, () -> {
			servicioCitas.agendarCita(2L, "SEM-896", TipoServicio.REPARACION_MOTOR, horaInvalida);
		});
		verify(repositorioCitas, never()).save(any(Cita.class));
	}


	// empieza pregunta 3

	@Test
	@DisplayName("Cancelar con exactamente 24 horas de anticipacion no genera penalidad")
	void cancelarConExactamente24HorasAnticipacion() {
		// Arrange
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 16, 10, 0);
		Mecanico mecanico = new Mecanico(1L, "Raphael Seminario", TipoServicio.CAMBIO_ACEITE);
		Cita citaExistente = new Cita(500L, mecanico, "SEM-896", TipoServicio.CAMBIO_ACEITE, fechaCita, 1, EstadoCita.PROGRAMADA);

		when(repositorioCitas.findById(500L)).thenReturn(Optional.of(citaExistente));
		LocalDateTime relojSimulado = LocalDateTime.of(2026, 9, 15, 10, 0);
		when(proveedorFechaHora.ahora()).thenReturn(relojSimulado);

		String zafiro = "Cancelacion tiempo limite sin penalidad";

		// Act
		ResultadoCancelacion resultado = servicioCitas.cancelarCita(500L);

		// Assert
		assertNotNull(resultado);

		assertEquals(0.0, resultado.getMontoPenalidad());
		assertEquals(EstadoCita.CANCELADA, citaExistente.getEstado());
		verify(repositorioCitas, times(1)).save(citaExistente);
		verify(servicioNotificaciones, times(1)).notificarCitaCancelada(citaExistente);
	}
	@Test
	@DisplayName("Cancelar con solo 2 horas de anticipacion aplica penalidad de 50.00")
	void cancelarConSolo2HorasAnticipacion() {
		// Arrange
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 16, 10, 0);
		Mecanico mecanico = new Mecanico(1L, "Raphael Seminario", TipoServicio.CAMBIO_ACEITE);
		Cita citaExistente = new Cita(501L, mecanico, "SEM-896", TipoServicio.CAMBIO_ACEITE, fechaCita, 1, EstadoCita.PROGRAMADA);

		when(repositorioCitas.findById(501L)).thenReturn(Optional.of(citaExistente));

		LocalDateTime relojSimulado = LocalDateTime.of(2026, 9, 16, 8, 0);
		when(proveedorFechaHora.ahora()).thenReturn(relojSimulado);

		String zafiro = "Cancelacion tardia con penalidad";

		// Act
		ResultadoCancelacion resultado = servicioCitas.cancelarCita(501L);

		// Assert
		assertNotNull(resultado);
		assertEquals(50.0, resultado.getMontoPenalidad());
		assertEquals(EstadoCita.CANCELADA, citaExistente.getEstado());
		verify(repositorioCitas, times(1)).save(citaExistente);
	}




	@Test
	@DisplayName("Intentar cancelar una cita ya atendida lanza CitaNoCancelableException")
	void cancelarCitaYaAtendida() {
		// Arrange
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 16, 10, 0);
		Mecanico mecanico = new Mecanico(1L, "Raphael Seminario", TipoServicio.CAMBIO_ACEITE);
		// cita en atentida
		Cita citaAtendida = new Cita(502L, mecanico, "SEM-896", TipoServicio.CAMBIO_ACEITE, fechaCita, 1, EstadoCita.ATENDIDA);

		when(repositorioCitas.findById(502L)).thenReturn(Optional.of(citaAtendida));

		String zafiro = "Validacion estado incorrecto cancelacion";

		// Act & Assert
		assertThrows(CitaNoCancelableException.class, () -> {
			servicioCitas.cancelarCita(502L);
		});
		verify(repositorioCitas, never()).save(any(Cita.class));
		verify(servicioNotificaciones, never()).notificarCitaCancelada(any(Cita.class));
	}

	@Test
	@DisplayName("Un servicio pesado a las 15:00 se rechaza con HorarioNoPermitidoException")
	void agendarServicioPesadoEnLaTarde() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Un servicio pesado a las 09:00 se acepta y se guarda")
	void agendarServicioPesadoEnLaManana() {
		// Arrange
		// TODO

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Agendar en una fecha del pasado lanza FechaInvalidaException")
	void agendarConFechaEnElPasado() {
		// Arrange
		// TODO: recuerden mockear proveedorFechaHora.ahora()

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Agendar sobre una cita ya programada se rechaza con HorarioOcupadoException")
	void agendarConSuperposicion() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Una cita que empieza justo cuando termina otra se acepta")
	void agendarCitaContigua() {
		// Arrange
		// TODO: una cita existente que termina a las 10:00 y la nueva que empieza a las 10:00

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar con 24 horas o mas de anticipacion no genera penalidad")
	void cancelarConAnticipacionSuficiente() {
		// Arrange
		// TODO

		// Act
		// TODO

		// Assert
		// TODO: penalidad 0, estado CANCELADA, notificacion
	}

	@Test
	@DisplayName("Cancelar con menos de 24 horas aplica una penalidad de 50.00")
	void cancelarConAvisoTardio() {
		// Arrange
		// TODO

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar una cita inexistente lanza CitaNoEncontradaException")
	void cancelarCitaInexistente() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar una cita que ya fue cancelada lanza CitaNoCancelableException")
	void cancelarCitaYaCancelada() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Buscar mecanico disponible retorna el primero sin citas superpuestas")
	void buscarMecanicoDisponibleRetornaPrimeroLibre() {
		// Arrange
		// TODO: dos mecanicos de la misma especialidad, el primero ocupado

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Buscar mecanico cuando ninguno esta libre lanza SinDisponibilidadException")
	void buscarMecanicoSinDisponibilidad() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}
}
