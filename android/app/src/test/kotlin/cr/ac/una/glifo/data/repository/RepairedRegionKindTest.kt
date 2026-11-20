package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.domain.model.RegionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepairedRegionKindTest {

    @Test
    fun `maps the kind corrected by N2 and ignores unknown values`() {
        assertEquals(RegionKind.Math, repairedRegionKind("MATH"))
        assertEquals(RegionKind.Text, repairedRegionKind(" text "))
        assertEquals(RegionKind.Drawing, repairedRegionKind("DRAWING"))
        assertNull(repairedRegionKind(""))
        assertNull(repairedRegionKind(null))
        assertNull(repairedRegionKind("FIGURE"))
    }
}
