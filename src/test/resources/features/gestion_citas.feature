# language: en
Feature: Gestion de citas del taller mecanico

  Scenario: 1. Registro exitoso de MANTENIMIENTO_LIGERO con otro mecanico
    Given que el taller simula que hoy es "2026-09-15T08:00:00"
    And un mecanico "Carlos Perez" con id 2 y especialidad "MANTENIMIENTO_LIGERO" libre
    When se solicita registrar para la placa "SEM-896" el servicio "MANTENIMIENTO_LIGERO" el "2026-09-16T10:00:00" con id mecanico 2
    Then la cita queda "PROGRAMADA" y se notifica el agendamiento

  Scenario: 2. Intento de registro con el mecanico ocupado iniciando a las 11:00
    Given que el taller simula que hoy es "2026-09-15T08:00:00"
    And el mecanico "Raphael Seminario" con id 1 tiene una cita programada de "10:00" a "12:00"
    When se intenta registrar para la placa "SEM-896" el servicio "CAMBIO_ACEITE" el "2026-09-16T11:00:00" con id mecanico 1
    Then el sistema lo rechaza lanzando HorarioOcupadoException

  Scenario: 3. Intento de registro con el mecanico ocupado iniciando a las 12:00
    Given que el taller simula que hoy es "2026-09-15T08:00:00"
    And el mecanico "Raphael Seminario" con id 1 tiene una cita programada de "10:00" a "12:00"
    When se solicita registrar para la placa "SEM-896" el servicio "CAMBIO_ACEITE" el "2026-09-16T12:00:00" con id mecanico 1
    Then la cita queda "PROGRAMADA" y se notifica porque inicia justo al terminar la otra
