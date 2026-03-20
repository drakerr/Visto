package com.aleixcos.visto.ghostrun

import com.aleixcos.visto.domain.GhostRunSnapshot
import com.aleixcos.visto.domain.RunEvent

object GhostRunPlayer {

    // Recibe el snapshot actual y el tick — devuelve el nuevo snapshot
    // Sin estado interno, completamente puro
    fun snapshotAt(current: GhostRunSnapshot, tick: Long): GhostRunSnapshot {
        // En el MVP el ghost snapshot viene del servidor ya calculado
        // Este método es el punto de extensión para el playback completo
        return current
    }
}