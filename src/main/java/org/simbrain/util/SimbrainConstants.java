/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * A class solely for storing constants used throughout simbrain.
 *
 * @author Zoë Tosi
 */
public class SimbrainConstants {

    /**
     * Null string used in various parts of the GUI.
     */
    public static final String NULL_STRING = "...";

    /**
     * Convenience constant referring to current Locale.
     */
    public static final NumberFormat LOCAL_FORMATTER = NumberFormat.getNumberInstance(Locale.getDefault());

    /**
     * Used to indicate if an object (in particular neurons) have a polarity,
     * i.e. are specifically excitatory or inhibitory.  Convenience methods
     * included so that values passed in respect the object's polarity.
     */
    public static enum Polarity {
        EXCITATORY {
            @Override
            public double value(double val) {
                return Math.abs(val);
            }

            @Override
            public String title() {
                return "Excitatory";
            }
        }, INHIBITORY {
            @Override
            public double value(double val) {
                return -Math.abs(val);
            }

            @Override
            public String title() {
                return "Inhibitory";
            }

        }, BOTH {
            @Override
            public double value(double val) {
                return val;
            }

            @Override
            public String title() {
                return "None";
            }
        };

        /**
         * Get the appropriate value, e.g. excitatory for -5 is 5.
         *
         * @param val the value to check
         * @return the appropriate value
         */
        public abstract double value(double val);

        /**
         * The appropriate name for the enum member, for use in the GUI.
         * Mainly just capitalizes.
         *
         * @return the  name of enum member.
         */
        public abstract String title();
        
        @Override
        public String toString() {
            return title();
        }
    }

}
