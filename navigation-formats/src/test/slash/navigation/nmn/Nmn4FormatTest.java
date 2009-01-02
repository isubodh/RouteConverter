/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.nmn;

import slash.navigation.NavigationTestCase;

public class Nmn4FormatTest extends NavigationTestCase {
    Nmn4Format format = new Nmn4Format();

    public void testIsPosition() {
        assertTrue(format.isPosition("-|-|-|-|-|-|-|-|-|-|7.00905|51.44329|-|"));
        assertTrue(format.isPosition("-|-|-|45128|S�dviertel|45128|Hohenzollernstrasse/L451|-|-|-|7.00905|51.44329|-|"));
        assertTrue(format.isPosition("-|-|-|58452|Witten|58452|Schloss Steinhausen|-|-|-|-|-|460|"));

        // GPSBabel creates this
        assertTrue(format.isPosition("-|-|-|-|-|-|-|-|-|-|-|6.42323|51.84617|-|-|"));
        assertTrue(format.isPosition("-|-|16|-|-|bei D 22929,K�thel; Kr Hzgt Lauenburg,,0,|-|-|-|-|-|10.51239|53.61192|-|-|"));
    }

    public void testParsePositionWithStreet() {
        NmnPosition position = format.parsePosition("-|-|-|45128|S�dviertel|45128|Hohenzollernstrasse/L451|-|-|-|7.00905|51.44329|-|", null);
        assertEquals(7.00905, position.getLongitude());
        assertEquals(51.44329, position.getLatitude());
        assertEquals("45128 S�dviertel, Hohenzollernstrasse/L451", position.getComment());
        assertEquals("45128", position.getZip());
        assertEquals("S�dviertel", position.getCity());
        assertEquals("Hohenzollernstrasse/L451", position.getStreet());
        assertNull(position.getNumber());
    }

    public void testParseUppercaseComment() {
        NmnPosition position = format.parsePosition("-|-|-|45128|S�DVIERTEL|45128|HOHENZOLLERNSTRASSE|-|-|-|7.00905|51.44329|-|", null);
        assertEquals(7.00905, position.getLongitude());
        assertEquals(51.44329, position.getLatitude());
        assertEquals("45128 S�dviertel, Hohenzollernstrasse", position.getComment());
        assertEquals("45128", position.getZip());
        assertEquals("S�dviertel", position.getCity());
        assertEquals("Hohenzollernstrasse", position.getStreet());
        assertNull(position.getNumber());
    }

    public void testParseNegativePosition() {
        NmnPosition position = format.parsePosition("-|-|-|45128|S�dviertel|45128|Hohenzollernstrasse/L451|-|-|-|-7.00905|-51.44329|-|", null);
        assertEquals(-7.00905, position.getLongitude());
        assertEquals(-51.44329, position.getLatitude());
    }

    public void testParseGPSBabelPosition() {
        NmnPosition position = format.parsePosition("-|-|16|-|-|Linau|-|-|-|-|-|10.46348|53.64352|-|-|", null);
        assertEquals(10.46348, position.getLongitude());
        assertEquals(53.64352, position.getLatitude());
        assertEquals("Linau", position.getComment());
        assertNull(position.getZip());
        assertEquals("Linau", position.getCity());
        assertNull(position.getStreet());
        assertNull(position.getNumber());
    }


    public void testSetComment() {
        NmnPosition position = format.parsePosition("-|-|-|-|-|-|-|-|-|-|7.00905|51.44329|-|", null);
        assertEquals(7.00905, position.getLongitude());
        assertEquals(51.44329, position.getLatitude());
        assertNull(position.getCity());
        assertNull(position.getStreet());
        assertNull(position.getNumber());
        assertNull(position.getComment());
        assertTrue(position.isUnstructured());
        position.setComment(null);
        assertNull(position.getComment());
        position.setComment("Schelklingen, Marktstrasse 20");
        assertEquals("Schelklingen, Marktstrasse 20", position.getComment());
        assertEquals("Schelklingen", position.getCity());
        assertEquals("Marktstrasse", position.getStreet());
        assertEquals("20", position.getNumber());
        position.setComment("Bad Urach, Shell");
        assertEquals("Bad Urach", position.getCity());
        assertEquals("Shell", position.getStreet());
        assertEquals("Bad Urach, Shell", position.getComment());
        assertNull(position.getNumber());
    }
}