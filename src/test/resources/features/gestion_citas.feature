# language: en
Feature: Gestion de citas del taller mecanico

  Scenario: 1. Agendar un cambio de aceite de forma exitosa
    Given que "Raphael Seminario" atiende "CAMBIO_ACEITE" hoy "2026-09-15T08:00:00"
    When se agenda "CAMBIO_ACEITE" para "SEM-896" el "2026-09-16T10:00:00"
    Then la cita queda "PROGRAMADA" y se notifica

  Scenario: 2. Rechazar una reparacion de motor en la tarde
    Given que "Raphael Seminario" atiende "REPARACION_MOTOR" hoy "2026-09-15T08:00:00"
    When se agenda "REPARACION_MOTOR" para "SEM-896" el "2026-09-16T15:00:00"
    Then se lanza HorarioNoPermitidoException

  Scenario: 3. Cancelar con penalidad por aviso tardio
    Given una cita "PROGRAMADA" para "SEM-896" el "2026-09-16T10:00:00" y hoy es "2026-09-15T15:00:00"
    When se cancela la cita
    Then pasa a "CANCELADA" con multa de 50.00

  Scenario: 4. Rechazar un agendamiento por horario ocupado
    Given una cita "PROGRAMADA" para "SEM-896" el "2026-09-16T10:00:00" y hoy es "2026-09-15T08:00:00"
    When se agenda "CAMBIO_ACEITE" para "SEM-896" el "2026-09-16T10:30:00"
    Then se lanza HorarioOcupadoException