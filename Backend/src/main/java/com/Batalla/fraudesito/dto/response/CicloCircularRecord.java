package com.batalla.fraudesito.dto.response;

import java.util.List;

public record CicloCircularRecord(
        List<String> cuentasEnCiclo,
        Long saltos
) { }
