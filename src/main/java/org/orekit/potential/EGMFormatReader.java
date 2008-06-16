/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.potential;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;

/**This reader is adapted to the EGM Format.
 *
 * <p> The proper way to use this class is to call the {@link PotentialReaderFactory}
 *  which will determine which reader to use with the selected potential
 *  coefficients file <p>
 *
 * @see org.orekit.potential.PotentialReaderFactory
 * @author Fabien Maussion
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class EGMFormatReader extends PotentialCoefficientsReader {

    /** Format compatibility flag. */
    private boolean fileIsOK;

    /** The input to check and read. */
    private InputStream input;

    /** Simple constructor (the first method to call after construction is
     * {@link #isFileOK(InputStream)}. It is done automaticaly by the factory).
     */
    protected EGMFormatReader() {
        input = null;
        fileIsOK = false;
        ae = 6378136.3;
        mu = 398600.4415e8;
    }

    /** Check the file to determine if its format is understood by the reader or not.
     * @param in the input to check
     * @return true if it is readable, false if not.
     * @exception IOException when the {@link InputStream} cannot be buffered.
     */
    public boolean isFileOK(final InputStream in) throws IOException {

        this.input = in;
        final BufferedReader r = new BufferedReader(new InputStreamReader(in));

        // tests variables
        boolean iKnow = false;
        int lineIndex = 0;
        int c = 1;

        // set up the regular expressions
        final String integerField = " +[0-9]+";
        final String realField = " +[-+0-9.e.E]+";
        final Pattern regularPattern =
            Pattern.compile("^" + integerField + integerField +
                            realField + realField + realField + realField + " *$");

        // read the first lines to detect the format
        for (String line = r.readLine(); !iKnow; line = r.readLine()) {
            if (line == null) {
                iKnow = true;
            } else {
                final Matcher matcher = regularPattern.matcher(line);
                if (matcher.matches()) {
                    lineIndex++;
                }
                if ((lineIndex == c) && (c > 2)) {
                    iKnow = true;
                    fileIsOK = true;
                }
                if ((lineIndex != c) && (c > 2)) {
                    iKnow = true;
                    fileIsOK = false;
                }
                c++;
            }
        }
        return fileIsOK;
    }

    /** Computes the coefficients by reading the selected (and tested) file.
     * @exception OrekitException when the file has not been initialized or checked.
     * @exception IOException when the file is corrupted.
     */
    public void read() throws OrekitException, IOException {

        if (input == null) {
            throw new OrekitException("the reader has not been tested ",
                                      new Object[0]);
        }
        if (!fileIsOK) {
            throw new OrekitException("the reader is not adapted to the format ",
                                      new Object[0]);
        }

        final BufferedReader r = new BufferedReader(new InputStreamReader(input));
        final List cl = new ArrayList();
        final List sl = new ArrayList();
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            if (line.length() >= 15) {

                // get the fields defining the current the potential terms
                final String[] tab = line.trim().split("\\s+");
                final int i = Integer.parseInt(tab[0]);
                final int j = Integer.parseInt(tab[1]);
                final double c = Double.parseDouble(tab[2]);
                final double s = Double.parseDouble(tab[3]);

                // extend the cl array if needed
                while (cl.size() <= i) {
                    cl.add(new double[cl.size() + 1]);
                }

                // extend the sl array if needed
                while (sl.size() <= i) {
                    sl.add(new double[sl.size() + 1]);
                }

                // store the terms
                ((double[]) cl.get(i))[j] = c;
                ((double[]) sl.get(i))[j] = s;

            }
        }

        // convert to simple triangular arrays
        normalizedC = (double[][]) cl.toArray(new double[cl.size()][]);
        normalizedS = (double[][]) sl.toArray(new double[sl.size()][]);

    }

}
