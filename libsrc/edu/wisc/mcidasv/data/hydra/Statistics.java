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

import static edu.wisc.ssec.mcidasv.data.hydra.StatsTable.fmtMe;
import static java.util.Arrays.asList;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import visad.Data;
import visad.FlatField;
import visad.FunctionType;
import visad.MathType;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.TupleType;
import visad.VisADException;

public class Statistics {

    private static final List<Character> CHARS = asList('\u2581', '\u2582', '\u2583', '\u2584', '\u2585', '\u2586', '\u2587', '\u2588');

   DescriptiveStatistics[] descriptiveStats = null;
   double[][] values_x;
   double[][] rngVals;
   int rngTupLen;
   int numPoints;
   int[] numGoodPoints;
   MathType statType;

   PearsonsCorrelation pCorrelation = null;


   public Statistics(FlatField fltFld) throws VisADException, RemoteException {
      rngVals = fltFld.getValues(false);
      rngTupLen = rngVals.length;
      numPoints = fltFld.getDomainSet().getLength();
      numGoodPoints = new int[rngTupLen];

      values_x = new double[rngTupLen][];

      for (int k=0; k<rngTupLen; k++) {
        values_x[k] = removeMissing(rngVals[k]);
        numGoodPoints[k] = values_x[k].length;
      }

      descriptiveStats = new DescriptiveStatistics[rngTupLen];
      for (int k=0; k<rngTupLen; k++) {
        descriptiveStats[k] = new DescriptiveStatistics(values_x[k]);
      }

      MathType rangeType = ((FunctionType)fltFld.getType()).getRange();

      if (rangeType instanceof RealTupleType) {
        RealType[] rttypes = ((TupleType)rangeType).getRealComponents();
        if (rngTupLen > 1) {
          statType = new RealTupleType(rttypes);
        }
        else {
          statType = (RealType) rttypes[0];
        }
      }
      else if (rangeType instanceof RealType) {
        statType = (RealType) rangeType;
      }
      else {
         throw new VisADException("incoming type must be RealTupleType or RealType");
      }

      pCorrelation = new PearsonsCorrelation();
   }

   /** get the number of points in the domain of the FlatField
   *
   * @return number of points
   */
   public int numPoints() {
     return numPoints;
   }

   /** get the number of non-missing points in each range component
   *
   * @return number of non-missing points
   */
   public int[] getNumGoodPoints() {
     return numGoodPoints;
   }

   /* get the original range values
   *
   *@return the original range values
   */
   public double[][] getRngVals() {
     return rngVals;
   }

   /* get the range values actually used (missing removed)
   *
   *@return range values used
   */

   public double[][] getValues() {
     return values_x;
   }

   private double[] removeMissing(double[] vals) {
     int num = vals.length;
     int cnt = 0;
     int[] good = new int[num];
     for (int k=0; k<num; k++) {
        if ( !(Double.isNaN(vals[k])) ) {
          good[cnt] = k;
          cnt++;
        }
     }

     if (cnt == num) {
        return vals;
     }

     double[] newVals = new double[cnt];
     for (int k=0; k<cnt; k++) {
       newVals[k] = vals[good[k]];
     }

     return newVals;
   }

   private double[][] removeMissing(double[][] vals) {
     int tupLen = vals.length;
     double[][] newVals = new double[tupLen][];
     for (int k=0; k < tupLen; k++) {
        newVals[k] = removeMissing(vals[k]);
     }
     return newVals;
   }

   public Data mean() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getMean();
     }
     return makeStat(stats);
   }

   public Data geometricMean() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getGeometricMean();
     }
     return makeStat(stats);
   }


   public Data max() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getMax();
     }
     return makeStat(stats);
   }

   public Data min() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getMin();
     }
     return makeStat(stats);
   }

   public Data median() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getPercentile(50.0);
     }
     return makeStat(stats);
   }

    public Data percentile(double p) throws VisADException, RemoteException {
        double[] stats = new double[rngTupLen];
        for (int k=0; k<rngTupLen; k++) {
            stats[k] = descriptiveStats[k].getPercentile(p);
        }
        return makeStat(stats);
    }

   public Data variance() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getVariance();
     }
     return makeStat(stats);
   }

   public Data kurtosis() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getKurtosis();
     }
     return makeStat(stats);
   }

   public Data standardDeviation() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getStandardDeviation();
     }
     return makeStat(stats);
   }

   public Data skewness() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getSkewness();
     }
     return makeStat(stats);
   }

   public Data correlation(FlatField fltFld) throws VisADException, RemoteException {
     double[][] values_x = this.rngVals;
     double[][] values_y = fltFld.getValues(false);

     if (values_y.length != rngTupLen) {
       throw new VisADException("both fields must have same range tuple length");
     }

     double[] stats = new double[rngTupLen];
     
     for (int k=0; k<rngTupLen; k++) {
       double[][] newVals = removeMissingAND(values_x[k], values_y[k]);
       stats[k] = pCorrelation.correlation(newVals[0], newVals[1]);
     }

     return makeStat(stats);
   }

   private Data makeStat(double[] stats) throws VisADException, RemoteException {
     if (statType instanceof RealType) {
       return new Real((RealType)statType, stats[0]);
     }
     else if (statType instanceof RealTupleType) {
       return new RealTuple((RealTupleType)statType, stats);
     }
     return null;
   }

   private double[][] removeMissingAND(double[] vals_x, double[] vals_y) {
     int cnt = 0;
     int[] good = new int[vals_x.length];
     for (int k=0; k<vals_x.length; k++) {
       if ( !(Double.isNaN(vals_x[k])) && !(Double.isNaN(vals_y[k]))  ) {
         good[cnt] = k;
         cnt++;
       }
     }

     if (cnt == vals_x.length) {
       return new double[][] {vals_x, vals_y};
     }
     else {
       double[] newVals_x = new double[cnt];
       double[] newVals_y = new double[cnt];
       for (int k=0; k<cnt; k++) {
         newVals_x[k] = vals_x[good[k]];
         newVals_y[k] = vals_y[good[k]];
       }
       return new double[][] {newVals_x, newVals_y};
     }
   }

    public static Long[] histogram(FlatField field, int bins) throws VisADException, RemoteException {
        Long[] histogram = new Long[bins];
        EmpiricalDistribution distribution = new EmpiricalDistribution(bins);
        distribution.load(field.getValues(false)[0]);
        int k = 0;
        for (SummaryStatistics stats: distribution.getBinStats()) {
            histogram[k++] = stats.getN();
        }
        return histogram;
    }

    public static String describe(FlatField field) throws VisADException, RemoteException {
        StringBuilder sb = new StringBuilder(1024);
        Statistics s = new Statistics(field);
        double max = ((Real)s.max()).getValue();
        double min = ((Real)s.min()).getValue();
        double q1 = ((Real)s.percentile(25.0)).getValue();
        double q3 = ((Real)s.percentile(75.0)).getValue();
        double[] modes = StatUtils.mode(field.getValues(false)[0]);

        StringBuilder tmp = new StringBuilder(128);
        for (int i = 0; i < modes.length; i++) {
            tmp.append(fmtMe(modes[i]));
            if ((i+1) < modes.length) {
                tmp.append(", ");
            }
        }
        
        char endl = '\n';
        sb.append("Histogram :  ").append(sparkline(field, s)).append(endl)
            .append("Length    :  ").append(String.format("%d", s.numPoints())).append(endl)
            .append("Min       :  ").append(fmtMe(((Real) s.min()).getValue())).append(endl)
            .append("Max       :  ").append(fmtMe(((Real) s.max()).getValue())).append(endl)
            .append("Range     :  ").append(fmtMe(max - min)).append(endl)
            .append("Q1        :  ").append(fmtMe(q1)).append(endl)
            .append("Q2        :  ").append(fmtMe(((Real)s.percentile(50.0)).getValue())).append(endl)
            .append("Q3        :  ").append(fmtMe(q3)).append(endl)
            .append("IQR       :  ").append(fmtMe(q3 - q1)).append(endl)
            .append("Mean      :  ").append(fmtMe(((Real)s.mean()).getValue())).append(endl)
            .append("Mode      :  ").append(tmp.toString()).append(endl)
            .append("Kurtosis  :  ").append(fmtMe(((Real)s.kurtosis()).getValue())).append(endl)
            .append("Skewness  :  ").append(fmtMe(((Real)s.skewness()).getValue())).append(endl)
            .append("Std Dev   :  ").append(fmtMe(((Real)s.standardDeviation()).getValue())).append(endl)
            .append("Variance  :  ").append(fmtMe(((Real)s.variance()).getValue())).append(endl)
            .append("# Good Pts:  ").append(String.format("%d", s.getNumGoodPoints()[0])).append(endl);
        return sb.toString();
    }

    public static String describe(FlatField... fields) throws VisADException, RemoteException {
        // 350 is just slightly more than required
        StringBuilder buf = new StringBuilder(350 * fields.length);
        for (FlatField field : fields) {
            buf.append(describe(field)).append('\n');
        }
        return buf.toString();
    }

    public static String sparkline(FlatField field, Statistics s) throws VisADException, RemoteException {
        Long[] values = histogram(field, 20);
        Real sMin = (Real) s.min();
        Real sMax = (Real) s.max();
        Collection<Long> collection = asList(values);
        long max = Collections.max(collection);
        long min = Collections.min(collection);
        float scale = (max - min) / 7f;
        final StringBuilder buf = new StringBuilder(values.length);
        
        // TJJ Mar 2018 - sandwich with min/max
        // http://mcidas.ssec.wisc.edu/inquiry-v/?inquiry=2548
        buf.append(fmtMe((sMin).getValue()));
        for (Long value : values) {
            int index = Math.round((value - min) / scale);
            buf.append(CHARS.get(index));
        }
        buf.append(fmtMe((sMax).getValue()));
        
        return buf.toString();
    }

    public static String sparkline(FlatField... fields) throws VisADException, RemoteException {
        // assuming sparkline is only using 20 bins
        StringBuilder sb = new StringBuilder(25 * fields.length);
        for (FlatField field : fields) {
            Statistics s = new Statistics(field);
            sb.append(sparkline(field, s)).append('\n');
        }
        return sb.toString();
    }
}
