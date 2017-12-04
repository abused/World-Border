package com.wimbli.WorldBorder.forge;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilTest
{
    @Test
    public void testReplaceAmpColors() throws Exception
    {
        String resultA = Util.replaceAmpColors("&4Red &2Green &&9 Blue");
        String resultB = Util.replaceAmpColors("§a&B§c&d§e&F");
        String resultC = Util.replaceAmpColors("more & more");

        assertEquals("§4Red §2Green &§9 Blue", resultA);
        assertEquals("§a§B§c§d§e§F", resultB);
        assertEquals("more & more", resultC);
    }

    @Test
    public void testRemoveFormatting() throws Exception
    {
        String resultA = Util.removeFormatting("§4Red §2Green &§9 Blue");
        String resultB = Util.removeFormatting("§a§B§c§d§e§F");
        String resultC = Util.removeFormatting("§ 10000 in the bank");

        assertEquals("Red Green & Blue", resultA);
        assertEquals("", resultB);
        assertEquals("§ 10000 in the bank", resultC);
    }
}