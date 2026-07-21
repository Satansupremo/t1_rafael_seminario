# language: es
Feature: Gestion de citas del taller mecanico

  Escenario: 1. Agendar un cambio de aceite de forma exitosa
    Dado que "Raphael Seminario" atiende "CAMBIO_ACEITE" hoy "2026-09-15T08:00:00"
    Cuando se agenda "CAMBIO_ACEITE" para "SEM-896" el "2026-09-16T10:00:00"
    Entonces la cita queda "PROGRAMADA" y se notifica

  Escenario: 2. Rechazar una reparacion de motor en la tarde
    Dado que "Raphael Seminario" atiende "REPARACION_MOTOR" hoy "2026-09-15T08:00:00"
    Cuando se agenda "REPARACION_MOTOR" para "SEM-896" el "2026-09-16T15:00:00"
    Entonces se lanza HorarioNoPermitidoException

  Escenario: 3. Cancelar con penalidad por aviso tardio
    Dado una cita "PROGRAMADA" para "SEM-896" el "2026-09-16T10:00:00" y hoy es "2026-09-15T15:00:00"
    Cuando se cancela la cita
    Entonces pasa a "CANCELADA" con multa de 50.00

  Escenario: 4. Rechazar un agendamiento por horario ocupado
    Dado una cita "PROGRAMADA" para "SEM-896" el "2026-09-16T10:00:00" y hoy es "2026-09-15T08:00:00"
    Cuando se agenda "CAMBIO_ACEITE" para "SEM-896" el "2026-09-16T10:30:00"
    Entonces se lanza HorarioOcupadoException
