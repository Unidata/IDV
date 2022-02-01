/*
 * $Id: CacheDataSource.java,v 1.12 2007/08/17 20:34:15 jeffmc Exp $
 *
 * Copyright 1997-2022 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.data.adt;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public class FFT {

    private static int FFTBINS = 64;

    private static double fftReal[] = new double[FFTBINS];
    private static double fftComplex[] = new double[FFTBINS];
    private static double fftMagnitude[] = new double[FFTBINS];

    private FFT() {
    }

    /**
     * A Duhamel-Hollman split-radix dif FFT.
     *
     * Ref: Electronics Letters, Jan. 5, 1984 Complex input and output data in
     * arrays x and y Length is n. Inputs : RealArray_Input - Input data array
     * to perform FFT analysis CmplxArray_Input - Empty on input NumBins -
     * Number of histogram bins in input array Outputs : RealArray_Input - Real
     * part of FFT Analysis CmplxArray_Input - Complex part of FFT Analysis
     *
     * @return Values {@code <= 0} are errors, while anything {@code > 0} is ok.
     */
    private static int dfft() {
        /* real values array */
        double[] RealArr = new double[FFTBINS + 1];

        /* complex values array */
        double[] CmplxArr = new double[FFTBINS + 1];

        int LocalA;
        int LocalB;
        int LocalC;
        int LocalD;
        int LocalE;
        int LocalX0;
        int LocalX1;
        int LocalX2;
        int LocalX3;
        int LocalY;
        int LocalZ;
        int LocalE1;
        int LocalE2;
        int LocalE4;
        double LocalDblA;
        double LocalDblB;
        double LocalDblA3;
        double LocalDblX1;
        double LocalDblX3;
        double LocalDblY1;
        double LocalDblY3;
        double LocalDblW1;
        double LocalDblW2;
        double LocalDblZ1;
        double LocalDblZ2;
        double LocalDblZ3;
        double LocalDblXt;
        int i;

        RealArr[0] = 0.0;
        CmplxArr[0] = 0.0;
        for (i = 1; i <= FFTBINS; i++) {
            RealArr[i] = fftReal[i - 1];
            CmplxArr[i] = fftComplex[i - 1];
        }
        LocalA = 2;
        LocalD = 1;
        while (LocalA < FFTBINS) {
            LocalA = LocalA + LocalA;
            LocalD = LocalD + 1;
        }
        LocalE = LocalA;

        if (LocalE != FFTBINS) {
            for (LocalA = FFTBINS + 1; LocalA <= LocalE; LocalA++) {
                RealArr[LocalA] = 0.0;
                CmplxArr[LocalA] = 0.0;
            }
        }

        LocalE2 = LocalE + LocalE;
        for (LocalC = 1; LocalC <= LocalD - 1; LocalC++) {
            LocalE2 = LocalE2 / 2;
            LocalE4 = LocalE2 / 4;
            LocalDblB = 2.0 * PI / LocalE2;
            LocalDblA = 0.0;
            for (LocalB = 1; LocalB <= LocalE4; LocalB++) {
                LocalDblA3 = 3.0 * LocalDblA;
                LocalDblX1 = cos(LocalDblA);
                LocalDblY1 = sin(LocalDblA);
                LocalDblX3 = cos(LocalDblA3);
                LocalDblY3 = sin(LocalDblA3);
                LocalDblA = ((double) LocalB) * LocalDblB;
                LocalY = LocalB;
                LocalZ = 2 * LocalE2;
                while (LocalY < LocalE) {

                    for (LocalX0 = LocalY; LocalX0 <= LocalE - 1; LocalX0 = LocalX0 + LocalZ) {
                        LocalX1 = LocalX0 + LocalE4;
                        LocalX2 = LocalX1 + LocalE4;
                        LocalX3 = LocalX2 + LocalE4;
                        LocalDblW1 = RealArr[LocalX0] - RealArr[LocalX2];

                        RealArr[LocalX0] = RealArr[LocalX0] + RealArr[LocalX2];
                        LocalDblW2 = RealArr[LocalX1] - RealArr[LocalX3];
                        RealArr[LocalX1] = RealArr[LocalX1] + RealArr[LocalX3];
                        LocalDblZ1 = CmplxArr[LocalX0] - CmplxArr[LocalX2];
                        CmplxArr[LocalX0] = CmplxArr[LocalX0] + CmplxArr[LocalX2];
                        LocalDblZ2 = CmplxArr[LocalX1] - CmplxArr[LocalX3];
                        CmplxArr[LocalX1] = CmplxArr[LocalX1] + CmplxArr[LocalX3];
                        LocalDblZ3 = LocalDblW1 - LocalDblZ2;
                        LocalDblW1 = LocalDblW1 + LocalDblZ2;
                        LocalDblZ2 = LocalDblW2 - LocalDblZ1;
                        LocalDblW2 = LocalDblW2 + LocalDblZ1;
                        RealArr[LocalX2] = LocalDblW1 * LocalDblX1 - LocalDblZ2 * LocalDblY1;
                        CmplxArr[LocalX2] = -LocalDblZ2 * LocalDblX1 - LocalDblW1 * LocalDblY1;
                        RealArr[LocalX3] = LocalDblZ3 * LocalDblX3 + LocalDblW2 * LocalDblY3;
                        CmplxArr[LocalX3] = LocalDblW2 * LocalDblX3 - LocalDblZ3 * LocalDblY3;
                    }
                    LocalY = 2 * LocalZ - LocalE2 + LocalB;
                    LocalZ = 4 * LocalZ;
                }
            }
        }

        /*
         * ---------------------Last stage, length=2
         * butterfly---------------------
         */
        LocalY = 1;
        LocalZ = 4;
        while (LocalY < LocalE) {
            for (LocalX0 = LocalY; LocalX0 <= LocalE; LocalX0 = LocalX0 + LocalZ) {
                LocalX1 = LocalX0 + 1;
                LocalDblW1 = RealArr[LocalX0];
                RealArr[LocalX0] = LocalDblW1 + RealArr[LocalX1];
                RealArr[LocalX1] = LocalDblW1 - RealArr[LocalX1];
                LocalDblW1 = CmplxArr[LocalX0];
                CmplxArr[LocalX0] = LocalDblW1 + CmplxArr[LocalX1];
                CmplxArr[LocalX1] = LocalDblW1 - CmplxArr[LocalX1];
            }
            LocalY = 2 * LocalZ - 1;
            LocalZ = 4 * LocalZ;
        }

        /*
         * c--------------------------Bit reverse counter
         */
        LocalB = 1;
        LocalE1 = LocalE - 1;
        for (LocalA = 1; LocalA <= LocalE1; LocalA++) {
            if (LocalA < LocalB) {
                LocalDblXt = RealArr[LocalB];
                RealArr[LocalB] = RealArr[LocalA];
                RealArr[LocalA] = LocalDblXt;
                LocalDblXt = CmplxArr[LocalB];
                CmplxArr[LocalB] = CmplxArr[LocalA];
                CmplxArr[LocalA] = LocalDblXt;
            }
            LocalC = LocalE / 2;
            while (LocalC < LocalB) {
                LocalB = LocalB - LocalC;
                LocalC = LocalC / 2;
            }
            LocalB = LocalB + LocalC;
        }

        /* write Real/CmplxArr back to FFT_Real/Comples arrays */
        for (i = 1; i <= FFTBINS; i++) {
            fftReal[i - 1] = RealArr[i];
            fftComplex[i - 1] = CmplxArr[i];
        }

        return LocalE;

    }

    private static double complexAbs(double realValue, double imaginaryValue) {
        double storageValue;

        if (realValue < 0.0) {
            realValue = -realValue;
        }
        if (imaginaryValue < 0.0) {
            imaginaryValue = -imaginaryValue;
        }
        if (imaginaryValue > realValue) {
            storageValue = realValue;
            realValue = imaginaryValue;
            imaginaryValue = storageValue;
        }

        final double complexAbs;
        if ((realValue + imaginaryValue) == realValue) {
            complexAbs = realValue;
        } else {
            storageValue = imaginaryValue / realValue;
            storageValue = realValue * sqrt(1.0 + (storageValue * storageValue));
            complexAbs = storageValue;
        }
        return complexAbs;
    }

    public static int calculateFFT(double[] inputArray) {

        int fftValue = -99;

        for (int i = 0; i < FFTBINS; i++) {
            fftReal[i] = inputArray[i];
            fftComplex[i] = 0.0;
            fftMagnitude[i] = 0.0;
        }

        int retErr = FFT.dfft();
        if (retErr <= 0) {
            /* throw exception */
        } else {
            int harmonicCounter = 0;

            for (int i = 0; i < FFTBINS; i++) {
                fftMagnitude[i] = FFT.complexAbs(fftReal[i], fftComplex[i]);
                /*
                 * System.out.printf(
                 * "arrayinc=%d  FFT real=%f cmplx=%f magnitude=%f\n"
                 * ,i,fftReal[i],fftComplex[i],fftMagnitude[i]);
                 */
            }

            double fftTotalAllBins = 0.0;
            for (int i = 2; i <= 31; i++) {
                double fftBinM2 = fftMagnitude[i - 2];
                double fftBinM1 = fftMagnitude[i - 1];
                fftTotalAllBins = fftTotalAllBins + (fftBinM1 + fftBinM2) / 2.0;
                if ((fftMagnitude[i - 1] > fftMagnitude[i - 2])
                        && (fftMagnitude[i - 1] > fftMagnitude[i])) {
                    ++harmonicCounter;
                    /*
                     * System.out.printf("i=%d magnitude=%f  counter=%d\n",i,
                     * fftMagnitude[i],harmonicCounter);
                     */
                }
            }
            if (fftMagnitude[0] == 0) {
                /* throw exception */
            } else {
                fftValue = harmonicCounter;
            }
        }

        /* System.out.printf("Amplitude=%f\n",Amplitude); */
        return fftValue;
    }

}