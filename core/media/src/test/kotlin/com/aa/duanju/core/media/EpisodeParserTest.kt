package com.aa.duanju.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeParserTest {
    private val parser = EpisodeParser()

    @Test fun parsesCommonEpisodeNames() {
        assertEquals(12, parser.parse("第12集.mp4"))
        assertEquals(7, parser.parse("Drama_EP07.mkv"))
        assertEquals(3, parser.parse("episode 3.mov"))
        assertEquals(18, parser.parse("剧名-18.mp4"))
    }

    @Test fun fallsBackToLastNumberOrOne() {
        assertEquals(23, parser.parse("短剧2026版 23 完结.mp4"))
        assertEquals(1, parser.parse("大结局.mp4"))
    }

    @Test fun recognisesSupportedVideoExtensions() {
        assertTrue(parser.isVideo("A.WEBM"))
        assertTrue(parser.isVideo("B.m4v"))
        assertFalse(parser.isVideo("cover.jpg"))
    }
}
