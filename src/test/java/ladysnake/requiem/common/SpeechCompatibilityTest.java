package ladysnake.requiem.common;

import ladysnake.requiem.client.SpeechBubbleClientState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SpeechCompatibilityTest {

    @BeforeEach
    @AfterEach
    public void setup() {
        SpeechBubbleClientState.clear();
    }

    @Test
    public void testLanguageNormalization() {
        assertEquals("pt", SpeechTranslationService.normalizeTargetLanguage("pt_BR"));
        assertEquals("pt", SpeechTranslationService.normalizeTargetLanguage("pt-PT"));
        assertEquals("en", SpeechTranslationService.normalizeTargetLanguage("en_US"));
        assertEquals("es", SpeechTranslationService.normalizeTargetLanguage("es_ES"));
        assertEquals("he", SpeechTranslationService.normalizeTargetLanguage("iw"));
        assertEquals("id", SpeechTranslationService.normalizeTargetLanguage("in"));
        assertEquals("yi", SpeechTranslationService.normalizeTargetLanguage("ji"));
        assertEquals("en", SpeechTranslationService.normalizeTargetLanguage(null));
        assertEquals("en", SpeechTranslationService.normalizeTargetLanguage("   "));
        assertEquals("en", SpeechTranslationService.normalizeTargetLanguage("invalid1234"));
    }

    @Test
    public void testGoogleResponseParsing() throws Exception {
        String mockResponse = "[[[\"Olá mundo\",\"Hello world\",null,null,1]],null,\"en\",null,null,null,null,[]]";
        String translated = SpeechTranslationService.parseGoogleResponse(mockResponse);
        assertEquals("Olá mundo", translated);

        String multiChunk = "[[[\"Primeira parte. \",\"First part. \",null,null,1],[\"Segunda parte.\",\"Second part.\",null,null,1]],null,\"en\"]";
        assertEquals("Primeira parte. Segunda parte.", SpeechTranslationService.parseGoogleResponse(multiChunk));
    }

    @Test
    public void testClients5ResponseParsing() throws Exception {
        String mockClients5 = "[[\"Olá mundo\",\"en\"]]";
        String translated = SpeechTranslationService.parseClients5Response(mockClients5);
        assertEquals("Olá mundo", translated);
    }

    @Test
    public void testMyMemoryResponseParsing() throws Exception {
        String mockMyMemory = "{\"responseData\":{\"translatedText\":\"Hello World!\",\"match\":0.97}}";
        String translated = SpeechTranslationService.parseMyMemoryResponse(mockMyMemory);
        assertEquals("Hello World!", translated);
    }

    @Test
    public void testSpeechBubbleTypewriterAndGesture() {
        int entityId = 42;
        String message = "Hello";
        SpeechBubbleClientState.show(entityId, message);

        // At tick 0: 0 characters visible
        assertEquals("", SpeechBubbleClientState.visibleText(entityId));
        assertTrue(SpeechBubbleClientState.isGesturing(entityId));

        // Tick 2: 1 character visible ('H')
        SpeechBubbleClientState.tick();
        SpeechBubbleClientState.tick();
        assertEquals("H", SpeechBubbleClientState.visibleText(entityId));
        assertTrue(SpeechBubbleClientState.isGesturing(entityId));

        // Advance 8 more ticks (total 10 ticks = 5 chars 'Hello')
        for (int i = 0; i < 8; i++) {
            SpeechBubbleClientState.tick();
        }
        assertEquals("Hello", SpeechBubbleClientState.visibleText(entityId));
        assertTrue(SpeechBubbleClientState.isGesturing(entityId));

        // Text limited to 256 chars
        String longText = "a".repeat(300);
        SpeechBubbleClientState.show(99, longText);
        // After 256 * 2 = 512 ticks, all 256 characters are visible
        for (int i = 0; i < 512; i++) {
            SpeechBubbleClientState.tick();
        }
        assertEquals(256, SpeechBubbleClientState.visibleText(99).length());
        assertTrue(SpeechBubbleClientState.isGesturing(99));

        // After lifetime expired (> text.length * 2 + 52 ticks, i.e., > 564 ticks from start), bubble is removed
        for (int i = 0; i < 60; i++) {
            SpeechBubbleClientState.tick();
        }
        assertEquals("", SpeechBubbleClientState.visibleText(99));
        assertFalse(SpeechBubbleClientState.isGesturing(99));
    }
}
