package org.memegregator.core.collector;


import org.junit.jupiter.api.Test;

public class HtmlScrapperTest {

    @Test
    public void testScrapp(){
        HtmlScrapper scrapper = new HtmlScrapper("debasto.de", 0);
        scrapper.collectMemes();
    }
}
