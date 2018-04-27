/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2018
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.data.hydra;

public class IASI_L1C_Utility {

   public static int[][] ifov_order = new int[][] {new int[] {1,1}, new int[] {0,0}, new int[] {0,1}, new int[] {1,0}};
   public static int[][] ifov_order2 = new int[][] {new int[] {0,1}, new int[] {1,1}, new int[] {1,0}, new int[] {0,0}};
   public static int IDefScaleSondNbScale = 5;
   public static float IDefNsfirst1b      = 2581f;
   public static float IDefSpectDWn1b     = 25f;  // m-1

   public static float[] IDefScaleSondNsfirst = new float[] { 2581f,
                                                       5921f,
                                                       9009f,
                                                       9541f,
                                                      10721f};
                                                                                                                                                        
   public static float[] IDefScaleSondNslast = new float[] { 5920f,
                                                       9008f,
                                                       9540f,
                                                      10720f,
                                                      11041f};
                                                                                                                                                        
   public static float[] IDefSondScaleFactor = new float[] { 7f, 8f, 9f, 8f, 9f};
                                                                                                                                                        
                                                                                                                                                        
   public static float[] getIASILevel1CSpectralDomain(int num_spectra, float IDefSpectDWn1b, float IDefNsfirst1b, float[] spectrum)
   {
     if (spectrum == null) spectrum = new float[num_spectra];
                                                                                                                                                        
     for (int k=0; k<num_spectra; k++) {
       spectrum[k] = IDefSpectDWn1b*(IDefNsfirst1b+k-1);
     }
     return spectrum;
   }
                                                                                                                                                        
   public static float[] getIASILevel1CSpectralDomain(int num_spectra, float[] spectrum)
   {
     if (spectrum == null) spectrum = new float[num_spectra];

     for (int k=0; k<num_spectra; k++) {
       spectrum[k] = IDefSpectDWn1b*(IDefNsfirst1b+k-1);
     }
     return spectrum;
   }

   public static float[] getDecodedIASISpectra(short[] codedSpectra, int IDefScaleSondNbScale, float[] IDefSondScaleFactor, float[] IDefScaleSondNsfirst, float[] IDefScaleSondNslast, float IDefNsfirst1b, float[] decodedSpectra) {
     int num_spectra = codedSpectra.length;
     if (decodedSpectra == null) decodedSpectra = new float[num_spectra];

     for (int numScale=0; numScale<IDefScaleSondNbScale-1; numScale++) {
       float scale_factor = IDefSondScaleFactor[numScale];
       for (int chanNb = (int)IDefScaleSondNsfirst[numScale]; chanNb<(int)IDefScaleSondNslast[numScale]+1; chanNb++) {
         int w = chanNb - (int)IDefNsfirst1b;
         decodedSpectra[w] = 1.0E5f*codedSpectra[w]*((float)Math.pow(10.0, (double)-1*scale_factor));
       }
     }
     return decodedSpectra;
   }
                                                                                                                                                        
   public static float[] getDecodedIASISpectra(short[] codedSpectra, float[] decodedSpectra) {
     int num_spectra = codedSpectra.length;
     if (decodedSpectra == null) decodedSpectra = new float[num_spectra];

     for (int numScale=0; numScale<IDefScaleSondNbScale; numScale++) {
       float scale_factor = IDefSondScaleFactor[numScale];
       for (int chanNb = (int)IDefScaleSondNsfirst[numScale]; chanNb<(int)IDefScaleSondNslast[numScale]+1; chanNb++) {
         int w = chanNb - (int)IDefNsfirst1b;
         decodedSpectra[w] = 1.0E5f*codedSpectra[w]*((float)Math.pow(10.0, (double)-1*scale_factor));
         //-if ((visad.util.Util.isApproximatelyEqual(decodedSpectra[w], 0.0, 0.001))) decodedSpectra[w] += 0.005;
       }
     }
     return decodedSpectra;
   }

   public static float[] getDecodedIASIImage(short[] image, float[] decodedImage, int channelIndex) {
     if (decodedImage == null) decodedImage = new float[image.length];

     int scale_idx = -1;
     channelIndex += IDefNsfirst1b;
     for (int i=0; i<IDefScaleSondNbScale; i++) { 
        if ((channelIndex >= IDefScaleSondNsfirst[i]) && (channelIndex <= IDefScaleSondNslast[i])) {
          scale_idx = i;
        }
     }
     
     float scale_factor = IDefSondScaleFactor[scale_idx];

     for (int k=0; k<decodedImage.length; k++) {
       decodedImage[k] = 1.0E5f*image[k]*((float)Math.pow(10.0, (double)-1*scale_factor));
     }

     return decodedImage;
   }

   public static float[] psuedoScanReorder(float[] values, int numElems, int numLines) {
      float[] new_values = new float[values.length];

      for (int j=0; j<numLines/2; j++) { //- loop over EFOVs
        for (int i=0; i<numElems/2; i++) {
          int i2 = i*2;
          int j2 = j*2;
          for (int jj=0; jj<2; jj++) {  //- loop over IFOVs
            for (int ii=0; ii<2; ii++) {
              int k = jj*2 + ii;
              int idx_ma = (j2+ifov_order[k][0])*numElems + (i2+ifov_order[k][1]); //- idx_ma: mis-aligned
              int idx_a = (j2+jj)*numElems + i2+ii;  // idx_a: aligned
              new_values[idx_a] = values[idx_ma];
            }
          }
        }
      }
      return new_values;
   }

   public static float[] psuedoScanReorder2(float[] values, int numElems, int numLines) {
     float[] new_values = new float[values.length];
      for (int j=0; j<numLines/2; j++) { //- loop over EFOVs
        for (int i=0; i<numElems/2; i++) {
          int i2 = i*2;
          int j2 = j*2;
          for (int jj=0; jj<2; jj++) {  //- loop over IFOVs
            for (int ii=0; ii<2; ii++) {
              int k = jj*2 + ii;
              int idx_ma = j*(numElems*2) + i*4 + k;
              int idx_a = (j2+ifov_order2[k][0])*numElems + i2+ifov_order2[k][1];  // idx_a: aligned
              new_values[idx_a] = values[idx_ma];
            }
          }
        }
      }
      return new_values;
   }
}
