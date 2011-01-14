/*
 * $Id: IDV-Style.xjs,v 1.1 2006/05/03 21:43:47 dmurray Exp $
 * 
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.storm;


import ucar.unidata.data.storm.StormADOTInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Feb 26, 2009
 * Time: 10:02:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class StormADOTSceneType {

    // List<StormADOTInfo.RingData> tcircfirst_v72;

    /** _more_          */
    static int maxsec = 24;

    /** _more_          */
    static int maxsecA = 2000;

    /** _more_          */
    static double PI = 3.14159265358979323846;


    /**
     * _more_
     *
     * @param odtcurrent _more_
     * @param areadata _more_
     *
     * @return _more_
     */
    static float[] aodtv72_calcscene(StormADOTInfo.IRData odtcurrent,
                                     StormADOTInfo.DataGrid areadata )
    /* Perform Fast Fourier Transform (FFT) analysis and determine
       scene type via empirically defined threshold values.
        Inputs  : global structure odtcurrent_v72 containing current intensity values
        Outputs : various elements within structure odtcurrent_v72
        Return  : -41 : error with FFT routine
                  -51 : cloud temperature value <-100C or >+40C
                    0 : o.k.
    */
    {

        int                    nbin = 64, iok, a, b, bptr;
        int                    ixx, iyy, izz, iscene, idxcnt, maxsecd2,
                               sxscnt;
        int[]                  sectorcnt;
        int                    cnteye;
        float[]                bd, cbd, tbd;

        float                  radb, rade, radeye, teye, dx2,
                               eyecnt = 0,
                               rngcnt = 0;
        float                  xangle, xdist, xtemp, slice;
        float[]                sectordiffa, sectormin;
        float[][]              sector;
        float[]                eyearr, sxs;
        float                  sectang1, sectang2;
        float[]                avex, stdvx, skewx, xs, i2;
        float Aaveext, Astdvext, Askewext, Aavesym, Astdvsym, Askewsym;
        float                  Eaveeye, Estdveye, Eskeweye;
        float                  alsdist, alsradb, alsrade, alssum, alst,
                               alscnt;
        StormADOTInfo.RingData tcirc;

        float                  eyetemp;
        float                  eyecdosize, rmw;
        int                    eyecat;


        /* initialize temperature histogram bin values */
        bd  = new float[534];
        cbd = new float[534];
        tbd = new float[534];
        for (iyy = 0; iyy < nbin; iyy++) {
            bd[iyy] = 26.0f - (float) iyy * 2.0f;
        }
        /* set up arrays for FFT ananlysis.
         iscene=0 will perform FFT for cloud top region while
         iscene=1 will perform FFT for eye region */
        int cursorx = areadata.numx / 2;
        int cursory = areadata.numy / 2;
        List<StormADOTInfo.RingData> tcircfirst = aodtv72_readcirc(cursorx,
                                                      cursory, areadata);

        for (iscene = 0; iscene <= 1; iscene++) {
            for (iyy = 0; iyy < nbin; iyy++) {
                cbd[iyy] = 0.0f;
                tbd[iyy] = 0.0f;
            }

            /* define start and end radii values */
            if (iscene == 0) {
                /* CLOUD TOP */
                radb = (float) StormADOTInfo.kstart_v72;
                rade = (float) StormADOTInfo.kend_v72;
            } else {
                /* EYE REGION */
                radb = 0.0f;
                rade = (float) StormADOTInfo.kstart_v72;
            }

            /* load arrays for FFT analysis */
            Iterator iit = tcircfirst.iterator();

            while (iit.hasNext()) {
                tcirc = (StormADOTInfo.RingData) iit.next();
                if ((tcirc.dist >= radb) && (tcirc.dist <= rade)) {
                    teye = (tcirc.temp - 273.16f);
                    for (ixx = 0; ixx < (nbin - 1); ixx++) {
                        if ((teye <= bd[ixx]) && (teye >= bd[ixx + 1])) {
                            cbd[ixx] = cbd[ixx] + 1.0f;
                            tbd[ixx] = tbd[ixx] + teye;
                        }
                    }
                }
            }

            /* perform FFT analysis */
            float[] fftOut = aodtv72_fft(cbd);
            if (fftOut == null) {
                return null;         //return -41;
            }

            /* assign variables based upon region being analyzed */
            if (iscene == 0) {
                rngcnt = fftOut[1];  /* CLOUD TOP */
            } else {
                eyecnt = fftOut[1];  /* EYE REGION */
            }
        }

        /* compute various cloud and eye region parameters for classification scheme */

        /* allocate memory for arrays */

        eyearr    = new float[maxsecA];
        sectorcnt = new int[maxsec];
        sectormin = new float[maxsec];
        sector    = new float[maxsec][maxsecA];
        for (ixx = 0; ixx < maxsec; ixx++) {
            sectorcnt[ixx] = 0;
            sectormin[ixx] = 999.0f;
            for (iyy = 0; iyy < maxsecA; iyy++) {
                sector[ixx][iyy] = -999.99f;
            }
        }

        /* load array for analysis */
        radb   = (float) StormADOTInfo.kstart_v72;
        rade   = (float) StormADOTInfo.kend_v72;
        radeye = (float) StormADOTInfo.kstart_v72;
        Iterator it = tcircfirst.iterator();

        slice  = 360.0f / (float) maxsec;
        cnteye = 0;
        while (it.hasNext()) {
            tcirc  = (StormADOTInfo.RingData) it.next();
            xangle = tcirc.angle;
            xdist  = tcirc.dist;
            xtemp  = tcirc.temp - 273.16f;
            if (xangle == 360.0) {
                xangle = 0.0f;
            }
            ixx = 0;
            if ((xdist >= radb) && (xdist <= rade)) {
                while (ixx < maxsec) {
                    sectang1 = (float) Math.max(0.0, (float) ixx * slice);
                    sectang2 = (float) Math.min(360.0,
                            (float) (ixx + 1) * slice);
                    if ((xangle >= sectang1) && (xangle < sectang2)) {
                        sector[ixx][sectorcnt[ixx]] = xtemp;
                        sectorcnt[ixx]++;
                        ixx = maxsec;
                    } else {
                        ixx++;
                    }
                }
            }
            if ((xdist >= 0) && (xdist <= radeye)) {
                eyearr[cnteye] = xtemp;
                cnteye++;
            }
            /* the following will count all values w/in the different BD curve
               ranges for the whole cloud region so we can examine the structure
               of the cloud region later */
            //tcirc=tcirc->nextrec;
        }

        /* position annulus at CW max temp distance and
         determine mean temp w/in +/- 40km from this distance.  If dist
         is less than 68km from center, annulus will start at 28km */
        alscnt  = 0;
        alssum  = 0.0f;
        alsdist = (float) odtcurrent.cwring;
        alsradb = Math.max(28.0f, alsdist - 40.0f);
        alsrade = Math.max(108.0f, alsdist + 40.0f);
        it      = tcircfirst.iterator();
        while (it.hasNext()) {
            tcirc = (StormADOTInfo.RingData) it.next();
            xdist = tcirc.dist;
            xtemp = tcirc.temp - 273.16f;
            if ((xdist >= alsradb) && (xdist <= alsrade)) {
                alssum = alssum + xtemp;
                alscnt++;
            }
            //tcirc=tcirc->nextrec;
        }
        alst = alssum / (float) alscnt;
      //  odtcurrent.cloudt=alst;
      //  if ((odtcurrent.cloudt < -100.0) || (odtcurrent.cloudt > 40.0)) {
        if ((alst < -100.0) || (alst > 40.0)) {
            return null;  //return -51;
        }


        /* determine "minimum" temperature for each sector.  This is not the actual
         lowest temperature, but instead is the temperature value of the coldest
         90% of the values within the sector.  This will, hopefully, eliminate some
         outliers */

        /* allocate memory */
        xs  = new float[maxsecA];
        i2  = new float[maxsecA];
        sxs = new float[maxsecA];

        for (ixx = 0; ixx < maxsec; ixx++) {
            sxscnt = sectorcnt[ixx];
            for (iyy = 0; iyy < sxscnt; iyy++) {
                sxs[iyy] = sector[ixx][iyy];
            }
            aodtv72_xxsort(sxs, xs, i2, sxscnt);
            izz            = (int) (sxscnt - (sxscnt * .1f));
            sectormin[ixx] = xs[izz];
        }

        /* free memory */


        /* determine averages, standard deviations and skews for each sector */
        /* allocate memory */
        avex        = new float[maxsec];
        stdvx       = new float[maxsec];
        skewx       = new float[maxsec];
        sectordiffa = new float[maxsec];


        for (ixx = 0; ixx < maxsec; ixx++) {
            float[] out = aodtv72_calcskew(sector[ixx], sectorcnt[ixx]);
            avex[ixx]  = out[0];
            stdvx[ixx] = out[1];
            skewx[ixx] = out[2];
        }

        float[] out = aodtv72_calcskew(avex, maxsec);
        Aaveext  = out[0];
        Astdvext = out[1];
        Askewext = out[2];
       // odtcurrent.cloudt2=Aaveext;

        /* these are used to examine symmetry of convection */
        maxsecd2 = maxsec / 2;
        for (ixx = 0; ixx < maxsecd2; ixx++) {
            sectordiffa[ixx] = Math.abs(avex[ixx] - avex[maxsecd2 + ixx]);
        }
        out      = aodtv72_calcskew(sectordiffa, maxsecd2);
        Aavesym  = out[0];
        Astdvsym = out[1];
        Askewsym = out[2];
        /* these are used to examine properties of the eye region */
        out      = aodtv72_calcskew(eyearr, cnteye);
        Eaveeye  = out[0];
        Estdveye = out[1];
        Eskeweye = out[2];

        // odtcurrent.eyestdv=Estdveye;
        // odtcurrent.cloudsymave=Aavesym;
        // odtcurrent.eyefft=(int)eyecnt;
        // odtcurrent.cloudfft=(int)rngcnt;

        /* free memory */


        /* assign scenetype value in structure odtcurrent_v72 */

       return new  float [] { alst, Aaveext, Estdveye, Aavesym, eyecnt, rngcnt};


      //  return odt;
    }


    /**
     * _more_
     *
     * @param odtcurrent _more_
     * @param rmwsizeman _more_
     * @param areadata_v72 _more_
     * @param osstr_v72 _more_
     * @param osearch_v72 _more_
     *
     * @return _more_
     */

static     float[] aodtv72_classify(StormADOTInfo.IRData odtcurrent, int rmwsizeman,
                             StormADOTInfo.DataGrid areadata_v72,
                             float osstr_v72, boolean osearch_v72 )

    /* Classify scene type based on FFT analysis and histogram temperatures
       using empirically defined threshold values.
        Inputs  : global structure odtcurrent_v72 containing current image analysis
        Outputs : scenetype in structure odtcurrent_v72 is modified

        SCENE TYPES : EYE REGION         CLOUD REGION
                      0 - clear          0 - uniform
                      1 - pinhole        1 - embedded center
                      2 - large          2 - irregular cdo
                      3 - none           3 - curved band
                                         4 - shear
    */
    {

        int     iok, ixx, iyy,
                cloudfft = 0,
                cloudcat = 0, eyefft,
                eyecat   = 0,
                cwtcat   = 0, diffcat;
        int     cbring, cbringval, diffcloudcat;
        int     sceneeye, scenecloud,
                spiral = 0, spiralmax;
        int     lastcscene, lastescene, cbringvalmax;
        float   xlat, xlon, tempval, lasttno, lasttno12;
        float   eyetemp, cloudtemp, cloudcwt, eyestdv, cloudsyma;
        float   essizeDG = 0.0f,
                esdiffDG = 0.0f;
        float   lat1, lon1, lat2, lon2;
        float   eyecdosize, diffcloudt, rmw;
        float   cbringlatmax, cbringlonmax;
        double  curtime, curtimem12, xtime, lastvalidtime;

        boolean cbfound, cbscene, cbgray, shear, irrcdo, landcheck;
        float[] cdodiam = {
            0.0f, 85.0f, 140.0f, 195.0f, 250.0f, 999.0f
        };
        float   xpart, ddvor;
        int     cdocat;

        boolean embdd     = false,
                checkembc = false;
        float   Ea, Eb, Ec, Ed, Ee, eyeval, Ca, Cb, Cc, Cd, Ce, cloudval;
        float   diffeyecloudt, eyecloudcatdiff, eyecwtcatdiff, cloudcatdiff;
        float   eyepart   = 0,
                cloudpart = 0,
                cwtpart   = 0, diffcat2, lastvalidtno, lastr9, maxtno;
        float[] cdomax    = {
            0.0f, 0.4f, 0.4f, 0.5f, 0.5f, 0.6f, 0.6f
        };
        int     diffeyecloudcat;
        boolean foundeye, foundm12;


        eyetemp   = odtcurrent.eyet;
        eyefft    = odtcurrent.eyefft;
        eyestdv   = odtcurrent.eyestdv;
        cloudtemp = odtcurrent.cloudt;
        cloudcwt  = odtcurrent.cwcloudt;
        cloudfft  = odtcurrent.cloudfft;
        cloudsyma = odtcurrent.cloudsymave;
        xlat      = odtcurrent.latitude;
        xlon      = odtcurrent.longitude;

   // odtcurrent.eyestdv=Estdveye;
        // odtcurrent.cloudsymave=Aavesym;
        // odtcurrent.eyefft=(int)eyecnt;
        // odtcurrent.cloudfft=(int)rngcnt;

        for (ixx = 0; ixx < 10; ixx++) {
            /* compute cloud category */
            if ((cloudtemp <= StormADOTInfo.ebd_v72[ixx])
                    && (cloudtemp > StormADOTInfo.ebd_v72[ixx + 1])) {
                cloudcat = ixx;
                xpart =
                    (float) ((cloudtemp - StormADOTInfo.ebd_v72[cloudcat])
                             / (StormADOTInfo.ebd_v72[cloudcat + 1]
                                - StormADOTInfo.ebd_v72[cloudcat]));
                if (cloudcat == 0) {
                    xpart = 0.0f;
                }
                cloudpart = cloudcat + xpart;
            }
            /* compute eye category */
            if ((eyetemp <= StormADOTInfo.ebd_v72[ixx])
                    && (eyetemp > StormADOTInfo.ebd_v72[ixx + 1])) {
                eyecat = ixx;
                xpart = (float) ((eyetemp - StormADOTInfo.ebd_v72[eyecat])
                                 / (StormADOTInfo.ebd_v72[eyecat + 1]
                                    - StormADOTInfo.ebd_v72[eyecat]));
                if (eyecat == 0) {
                    xpart = 0.0f;
                }
                eyepart = eyecat + xpart;
            }
            /* compute C-W eye category */
            if ((cloudcwt <= StormADOTInfo.ebd_v72[ixx])
                    && (cloudcwt > StormADOTInfo.ebd_v72[ixx + 1])) {
                cwtcat = ixx;
                xpart = (float) ((cloudcwt - StormADOTInfo.ebd_v72[cwtcat])
                                 / (StormADOTInfo.ebd_v72[cwtcat + 1]
                                    - StormADOTInfo.ebd_v72[cwtcat]));
                if (cwtcat == 0) {
                    xpart = 0.0f;
                }
                cwtpart = cwtcat + xpart;
            }
        }

        /* printf("EYE = temp=%f cat=%d part=%f \n",eyetemp,eyecat,eyepart); */
        /* printf("CLD = temp=%f cat=%d part=%f \n",cloudtemp,cloudcat,cloudpart); */
        /* printf("CWT = temp=%f cat=%d part=%f \n",cloudcwt,cwtcat,cwtpart); */
        diffcat         = Math.max(0, Math.max(cloudcat, cwtcat) - eyecat);
        diffcloudt      = cloudtemp - cloudcwt;
        diffeyecloudt   = eyetemp - cloudtemp;
        eyecwtcatdiff   = cwtpart - eyepart;
        eyecloudcatdiff = cloudpart - eyepart;
        cloudcatdiff    = cloudpart - cwtpart;
        diffcloudcat    = cloudcat - cwtcat;
        diffeyecloudcat = cloudcat - eyecat;
        diffcat2        = eyetemp - (Math.min(cloudtemp, cloudcwt));

        curtime         = odtcurrent.date; //aodtv72_calctime(odtcurrent.date, odtcurrent.time);
        //curtimem12      = curtime - 0.5;

        /* determine last Final T# value for curved band/other scene check */
        foundeye     = false;
        maxtno       = 0.0f;
        lastr9       = 0;
        lasttno      = maxtno;
        lastvalidtno = lasttno;
        if (true) {  //(odthistoryfirst_v72==0)||(strlen(hfile_v72)==0)) {
            foundm12   = true;
            lastescene = 3;
            lastr9     = 1;
            if ((cwtpart < 3.5) && (osstr_v72 < 3.5)) {
                lastcscene = 3;
                lasttno12  = osstr_v72;
            } else {
                lastcscene = 0;
                lasttno12  = Math.max(osstr_v72, 4.0f);
            }
        }  /*else {
          odthistory=odthistoryfirst_v72;
          foundm12=FALSE;
          lastcscene=3;
          while(odthistory!=0) {
            xtime=aodtv72_calctime(odthistory->IR.date,odthistory->IR.time);
            landcheck=true;
            if(((oland_v72)&&(odthistory->IR.land==1))||(odthistory->IR.Traw<1.0)) landcheck=FALSE;
            if((xtime<curtime)&&(landcheck)) {
              lastvalidtime=xtime;
              if((xtime>=curtimem12)&&(!foundm12)) {
                lasttno12=odthistory->IR.Tfinal;
                foundm12=true;
              }
              lasttno=odthistory->IR.Tfinal;
              lastcscene=odthistory->IR.cloudscene;
              lastescene=odthistory->IR.eyescene;
              if(lastescene<=2) foundeye=true;
              if((lastcscene==4)&&(lastescene==3)) foundeye=FALSE;
              lastvalidtno=lasttno;
              lastr9=odthistory->IR.rule9;
              if(lasttno>maxtno) maxtno=lasttno;
            } else {
              if(!landcheck) {

                if((xtime-lastvalidtime)>0.5) {
                  foundeye=FALSE;
                  lasttno=lastvalidtno-(1.0*(xtime-lastvalidtime));

                }
              }
            }
            odthistory=odthistory->nextrec;
          }

          if(!foundm12) lasttno12=lasttno;
        } */
        /* printf("foundm12=%d\n",foundm12); */
        /* printf("lasttno12=%f\n",lasttno12); */

        /* NEW SCENE IDENTIFICATION SCHEME */
        /* NEW EYE SCENE THRESHOLD DETERMINATION */
        Ec = 0.0f;
        Ee = 0.0f;
        Ea = 1.0f - ((eyefft - 2) * 0.1f);
        Eb = -(eyepart * 0.5f);
        if (eyestdv > 10.0) {
            Ec = 0.50f;
        }
        /* Ed=eyecloudcatdiff*0.75; */
        Ed = (eyecloudcatdiff * 0.25f) + (eyecwtcatdiff * 0.50f);  /* new */
        /* printf("Ed=%f\n",Ed); */
        /* printf("maxtno=%f Ee=%f\n",maxtno,Ee); */
        if ((foundm12) && (lastescene < 3) && (maxtno > 5.0)) {
            Ee = Ee + 0.25f;
        }
        /* printf("Ee=%f\n",Ee); */
        /* if(lasttno12<3.5) Ee=Ee-1.0; */
        if (lasttno12 <= 4.5) {
            Ee = Math.max(-1.0f, lasttno12 - 4.5f);  /* new */
        }
        /* printf("lasttno12=%f  Ee=%f\n",lasttno12,Ee); */
        if ((lastr9 > 0) && (lasttno < 4.0)) {
            Ee = Ee - 0.5f;  /* new */
        }
        /* printf("Ee=%f\n",Ee); */
        eyeval   = Ea + Eb + Ec + Ed + Ee;
        sceneeye = 3;  /* NO EYE */
        if (eyeval >= 0.50) {
            sceneeye = 0;  /* EYE */
        }
        /*
          if(eyeval>=0.00) sceneeye=5;   / OBSCURED EYE /
          if(eyeval>=1.50) sceneeye=4;   / RAGGED EYE /
          if(eyeval>=3.50) sceneeye=0;   / CLEAR EYE /
        */

        /* printf("eyeval= %f  sceneeye=%d \n",eyeval,sceneeye); */
        float odtcurrent_v72IRRMW;
        eyecdosize = 0.0f;
        if (rmwsizeman > 0) {
            /* printf("RMW SIZE=%d\n",rmwsizeman_v72); */
            odtcurrent_v72IRRMW = (float) rmwsizeman;
            eyecdosize = (float) rmwsizeman - 1.0f;  /* manually input eye size */
        } else {
            float[] out = aodtv72_rmw(odtcurrent, areadata_v72);
            rmw                 = out[0];
            eyecdosize          = out[1];
            odtcurrent_v72IRRMW = rmw;
        }

        /* LARGE EYE CHECKS */
        if ((sceneeye == 0) && (eyecdosize >= 45.0)) {
            sceneeye = 2;  /* large eye */
        }

        /* NEW CLOUD SCENE THRESHOLD DETERMINATION */
        shear   = false;
        irrcdo  = false;
        cbscene = true;
        cbgray  = true;

        Cc      = 0.0f;
        Cd      = 0.5f;  /* changed to 0.5 */
        Ce      = 0.0f;
        Ca      = cwtpart * 0.25f;
        Cb      = cloudpart * 0.25f;
        if (cloudfft <= 2) {
            Cc = Math.min(1.50f, cwtpart * 0.25f);
        }
        if (lastcscene >= 3) {
            Cd = -0.50f;
        }
        /* printf("cwtpart=%f lasttno12=%f\n",cwtpart,lasttno12); */
        if (cwtpart > 2.0) {  /* new */
            if (lasttno12 >= 2.5) {
                if (sceneeye == 0) {
                    Ce = Math.min(1.00f, lasttno12 - 2.5f);
                }
                if (lasttno12 >= 3.5) {
                    Ce = Ce + 1.00f;
                }
            }
            if ((foundm12) && (foundeye)) {
                Ce = Ce + 1.25f;
            }
        }
        cloudval = Ca + Cb + Cc + Cd + Ce;
        if (cloudval < 0.0) {
            shear = true;  /* SHEAR */
        }
        if (cloudval >= 0.00) {
            cbscene = true;  /* CURVED BAND (gray) */
        }
        if (cloudval >= 1.00) {
            cbscene = true;     /* CURVED BAND (gray) */
            /* check for irregular CDO */
            if ((diffcat2 < 0.0) && (cloudsyma > 40.0)) {
                irrcdo = true;  /* IRREGULAR CDO */
            }
        }
        if ((cloudval >= 2.00) && (cloudval < 3.00)) {
            cbscene = true;          /* CURVED BAND (gray) */
            /* check for irregular CDO */
            /* if((diffcloudcat<0)&&(diffcloudt>8.0)&&(cloudsyma>30.0)) { */
            if ((diffcat2 < 0.0) && (cloudsyma > 30.0)) {
                irrcdo = true;       /* IRREGULAR CDO */
            }
            if (cwtcat >= 3) {
                /* if xcwt>3.0 try black/white CB check */
                if ((diffcloudcat > 0) && (diffcloudt < -8.0)) {
                    cbgray = false;  /* CURVED BAND (black/white) */
                }
                /* check for large/ragged eye */
                if ((sceneeye == 0)
                        || ((eyepart > 1.00) && (diffeyecloudcat >= 2.00))) {
                    cbscene = false;  /* EYE */
                }
                /* check for CDO */
                if ((cloudcatdiff <= 0.0) && (eyecwtcatdiff < 1.00)) {
                    cbscene = false;  /* CDO */
                }
            }
        }
        if (cloudval >= 3.00) {
            cbscene = false;  /* CDO */
            /* check for irregular CDO */
            if ((diffcloudcat < 0) && (diffcloudt > 8.0)
                    && (cloudsyma > 30.0)) {
                irrcdo  = true;  /* IRREGULAR CDO */
                cbscene = true;
            }
        }
        /* EMBEDDED CENTER CHECK */
        if ((cloudtemp < cloudcwt) && (cloudcwt < eyetemp)) {
            checkembc = true;
        }
        if (( !cbscene) && (checkembc)) {
            tempval = (float) StormADOTInfo.ebd_v72[cwtcat + 1] + 273.16f;
            float[] out = aodtv72_logspiral(xlat, xlon, tempval, 1,
                                            odtcurrent, areadata_v72);
            spiral = (int) out[0];
            if ((spiral >= 8) && (spiral < 20)) {
                embdd = true;
            }
            /* printf(" EMBDD : cwtcat=%d spiral=%d \n",cwtcat,spiral); */
        }

        /* printf("cloudval= %f  shear=%d cbscene=%d cbgray=%d irrcdo=%d \n",cloudval,shear,cbscene,cbgray,irrcdo); */
        //  (void)aodtv72_julian2cmonth(odtcurrent_v72IR.date,cdate);
        /* printf("%9s %6d
              %4.1f %4.1f %2d %2d %5.1f %5.1f  %5.2f %5.2f %5.2f %5.2f %5.2f %5.2f %5.2f
              %2d %2d %4.1f %4.1f  %5.2f %5.2f %5.2f %5.2f %5.2f %5.2f   %6.2f %7.2f  %3.1f  \n",cdate,odtcurrent_v72->IR.time,
              cloudpart,cwtpart,cloudfft,diffcloudcat,diffcloudt,cloudsyma,Ca,Cb,0.0,Cc,Cd,Ce,cloudval,
              eyefft,diffeyecloudcat,eyepart,eyestdv,Ea,Eb,Ec,Ed,Ee,eyeval,xlat,xlon,lasttno); */

        /* CLASSIFY CLOUD REGION */
        cbring       = 0;
        cbringval    = 0;
        cbringvalmax = 0;
        cbringlatmax = xlat;
        cbringlonmax = xlon;

        // L100:
        if (cbscene) {
            if (shear) {
                sceneeye   = 3;  /* NO EYE */
                scenecloud = 4;  /* SHEAR */
                tempval    = (float) StormADOTInfo.ebd_v72[3] + 273.16f;
                float[] out = aodtv72_cdoshearcalc(xlat, xlon, tempval, 3,
                                  areadata_v72);
                eyecdosize = Math.max(4.0f, out[0]);
            } else if (irrcdo) {
                sceneeye   = 3;  /* NO EYE */
                scenecloud = 2;  /* IRREGULAR CDO */
            } else {
                cbfound = false;
                boolean cont = true;
                // L200:
                if (cbgray) {
                    /* perform Curved Band analysis */
                    ixx = 4;  /* start with LIGHT GRAY */
                    while ((ixx >= 2) && ( !cbfound) && cont) {
                        tempval = (float) StormADOTInfo.ebd_v72[ixx]
                                  + 273.16f;
                        float[] out0 = aodtv72_logspiral(xlat, xlon, tempval,
                                           1, odtcurrent, areadata_v72);
                        spiral = (int) out0[0];
                        /* printf("BD level=%d  spiral=%d\n",ixx,spiral); */
                        if ((out0[0] >= 8) || (ixx == 2)) {  /* 10 = .375% -- 10 ==> 9 arcs of 15 degrees */
                            if (spiral > 25) {
                                if (ixx == 4) {
                                    cbgray = false;
                                    //goto L200;
                                    cbscene = false;
                                    ixx     = 6;
                                    while ((ixx > 4) && ( !cbfound)) {
                                        tempval =
                                            (float) StormADOTInfo
                                                .ebd_v72[ixx] + 273.16f;
                                        float[] out = aodtv72_logspiral(xlat,
                                                          xlon, tempval, 1,
                                                          odtcurrent,
                                                          areadata_v72);
                                        spiral = (int) out[0];
                                        if ((spiral >= 9) && (spiral <= 25)) {
                                            cbfound = true;
                                        } else {
                                            ixx--;
                                        }
                                    }
                                } else {
                                    ixx = 0;
                                }
                            } else {
                                if ((ixx == 2) && (spiral < 7)) {  /* 7 = .25% -- 10 ==> 6 arcs of 15 degrees */
                                    /* probably shear */
                                    cbfound = false;
                                    shear   = true;
                                    // goto L100;
                                    sceneeye   = 3;  /* NO EYE */
                                    scenecloud = 4;  /* SHEAR */
                                    tempval =
                                        (float) StormADOTInfo.ebd_v72[3]
                                        + 273.16f;
                                    float[] out = aodtv72_cdoshearcalc(xlat,
                                                      xlon, tempval, 3,
                                                      areadata_v72);
                                    eyecdosize = Math.max(4.0f, out[0]);
                                    cont = false;
                                } else {
                                    cbfound = true;
                                }
                            }
                        } else {
                            ixx--;
                        }
                    }
                } else {
                    /* try BLACK and WHITE rings */
                    cbscene = false;
                    ixx     = 6;
                    while ((ixx > 4) && ( !cbfound)) {
                        tempval = (float) StormADOTInfo.ebd_v72[ixx]
                                  + 273.16f;
                        float[] out = aodtv72_logspiral(xlat, xlon, tempval,
                                          1, odtcurrent, areadata_v72);
                        spiral = (int) out[0];
                        if ((spiral >= 9) && (spiral <= 25)) {
                            cbfound = true;
                        } else {
                            ixx--;
                        }
                    }
                }
                if (cbfound && cont) {
                    /* found curved band scenes */
                    cbring     = ixx;
                    cbringval  = spiral;
                    sceneeye   = 3;  /* NO EYE */
                    scenecloud = 3;  /* CURVED BAND */
                    /* search for maximum curved band analysis location within 1-degree box */
                    tempval = (float) StormADOTInfo.ebd_v72[cbring] + 273.16f;
                    if (osearch_v72) {
                        float[] out = aodtv72_logspiral(xlat, xlon, tempval,
                                          2, odtcurrent, areadata_v72);
                        spiralmax    = (int) out[0];
                        lat2         = out[1];
                        lon2         = out[2];

                        cbringvalmax = (int) out[0];
                        cbringlatmax = out[1];
                        cbringlonmax = out[2];
                    }
                } else if(cont){
                    /* did not find curved band scenes, mark as non-eye/eye scene */
                    scenecloud = 0;
                    cbscene    = false;
                    embdd      = false;  /* redundant declaration */
                    // goto L100;
                    scenecloud = 0;      /* UNIFORM */
                    if (embdd) {
                        scenecloud = 1;  /* EMBEDDED CENTER */
                    }
                    /* PINHOLE EYE TEST */
                    /* printf("sceneeye=%d diffeyecloudcat=%d eyefft=%d cwtpart=%f scenecloud=%d cloudfft=%d lasttno12=%f\n",
                    sceneeye,diffeyecloudcat,eyefft,cwtpart,scenecloud,cloudfft,lasttno12); */
                    if ((rmwsizeman > 0) && (rmwsizeman < 5.0)) {
                        sceneeye = 1;
                    }
                    if ((eyeval > -0.25) && (eyeval < 1.50)
                            && (diffeyecloudcat >= 2) && (eyefft <= 2)
                            && (cwtpart > 6.0) && (scenecloud <= 1)
                            && (cloudfft <= 4) && (lasttno12 >= 3.5)) {
                        sceneeye = 1;  /* PINHOLE EYE CHECK */
                    }
                } else {
                    scenecloud = 4;  /* SHEAR */
                }
            }
        } else {
            scenecloud = 0;            /* UNIFORM */
            if (embdd) {
                scenecloud = 1;        /* EMBEDDED CENTER */
            }
            /* PINHOLE EYE TEST */
            /* printf("sceneeye=%d diffeyecloudcat=%d eyefft=%d cwtpart=%f scenecloud=%d cloudfft=%d lasttno12=%f\n",
            sceneeye,diffeyecloudcat,eyefft,cwtpart,scenecloud,cloudfft,lasttno12); */
            if ((rmwsizeman > 0) && (rmwsizeman < 5.0)) {
                sceneeye = 1;
            }
            if ((eyeval > -0.25) && (eyeval < 1.50) && (diffeyecloudcat >= 2)
                    && (eyefft <= 2) && (cwtpart > 6.0) && (scenecloud <= 1)
                    && (cloudfft <= 4) && (lasttno12 >= 3.5)) {
                sceneeye = 1;  /* PINHOLE EYE CHECK */
            }
        }
        if ((scenecloud <= 2) && (sceneeye == 3)) {
            /* for CDO TESTS */
            for (ixx = 2; ixx <= 6; ixx++) {  /* DG,MG,LG,B,W */
                tempval = (float) StormADOTInfo.ebd_v72[ixx] + 273.16f;
                /* printf("xlat=%f xlon=%f tempval=%f\n",xlat,xlon,tempval); */
                float[] out = aodtv72_cdoshearcalc(xlat, xlon, tempval, 1,
                                  areadata_v72);
                /* printf("CDO : ixx=%d  cdosize=%f  cdosize/111=%f  \n",ixx,cdosize,cdosize/111.0); */
                if (ixx == 2) {
                    eyecdosize = out[0];
                }
            }
        }

        /*  odtcurrent_v72IR.eyescene=sceneeye;
          odtcurrent_v72IR.cloudscene=scenecloud;
          odtcurrent_v72IR.eyesceneold=-1;
          odtcurrent_v72IR.cloudsceneold=-1;
          odtcurrent_v72IR.eyecdosize=eyecdosize;
          odtcurrent_v72IR.ringcb=cbring;
          odtcurrent_v72IR.ringcbval=cbringval;
          odtcurrent_v72IR.ringcbvalmax=cbringvalmax;
          odtcurrent_v72IR.ringcblatmax=cbringlatmax;
          odtcurrent_v72IR.ringcblonmax=cbringlonmax;
          */
        float[] odt = {
            sceneeye, scenecloud, eyecdosize, cbring, cbringval, cbringvalmax,
            cbringlatmax, cbringlonmax, odtcurrent_v72IRRMW};

        return odt;

    }


    /**
     *
     * @param odtcurrent
     * @param areadata
     * @return
     */


static     float[] aodtv72_rmw(StormADOTInfo.IRData odtcurrent,
                        StormADOTInfo.DataGrid areadata)
    /* Determine radius of maximum wind based upon Jim Kossin's
       regression based scheme
        Inputs  : ix0     - element location of center point
                  iy0     - line location of center point
        Outputs : rmw     - radius of maximum wind distance
                  eyesize - eye size radius (km)
                  -1 = error/eyewall not found
                   0 = radius found
    */
    {
        int   ixmin, ixmax, iymin, iymax;
        int   ixc, iyc, i, ix, iy,
              idx1 = 0,
              idy1 = 0,
              idx2 = 0,
              idy2 = 0;
        float dx1, dx2, dy1, dy2, dav, xlat, xlon, xclat, xclon, xangle;
        float tcrit = 228.0f,
              warm  = 223.0f;
        float rmw;
        float eyesize;

        /* calculate cursorx/cursory from numx/numy... values should be 0.5*numx/y */
        ixc   = areadata.numx / 2;
        iyc   = areadata.numy / 2;
        ixmax = Math.min(areadata.numx, ixc + 320);
        ixmin = Math.max(0, ixc - 320);
        iymax = Math.min(areadata.numy, iyc + 240);
        iymin = Math.max(0, iyc - 240);

        if (odtcurrent.cloudt >= warm) {
            tcrit = (odtcurrent.eyet + (2.0f * odtcurrent.cloudt)) / 3.0f;
        }

        rmw     = -99.9f;
        eyesize = -99.9f;
        /* iterate five times */
        for (i = 0; i < 5; i++) {
            ix = ixc;
            while (areadata.temp[iyc][ix] > tcrit) {
                ix = ix - 1;
                if (ix == ixmin) {
                    return new float []{-99.9f, -99.9f};
                }
            }
            idx1 = ix;
            ix   = ixc;
            while (areadata.temp[iyc][ix] > tcrit) {
                ix = ix + 1;
                if (ix == ixmax) {
                     return new float []{-99.9f, -99.9f};
                }
            }
            idx2 = ix;
            iy   = iyc;
            while (areadata.temp[iy][ixc] > tcrit) {
                iy = iy - 1;
                if (iy == iymin) {
                     return new float []{-99.9f, -99.9f};
                }
            }
            idy1 = iy;
            iy   = iyc;
            while (areadata.temp[iy][ixc] > tcrit) {
                iy = iy + 1;
                if (iy == iymax) {
                     return new float []{-99.9f, -99.9f};
                }
            }
            idy2 = iy;
            ixc  = (int) ((((float) (idx1 + idx2)) / 2.0));
            iyc  = (int) ((((float) (idy1 + idy2)) / 2.0));
        }
        xclat = areadata.lat[iyc][ixc];
        xclon = areadata.lon[iyc][ixc];
        xlat  = areadata.lat[iyc][idx1];
        xlon  = areadata.lon[iyc][idx1];
        float[] out = aodtv72_distance(xlat, xlon, xclat, xclon, 1);
        dx1  = out[0];

        xlat = areadata.lat[iyc][idx2];
        xlon = areadata.lon[iyc][idx2];
        out  = aodtv72_distance(xlat, xlon, xclat, xclon, 1);
        dx2  = out[0];
        xlat = areadata.lat[idy1][ixc];
        xlon = areadata.lon[idy1][ixc];
        out  = aodtv72_distance(xlat, xlon, xclat, xclon, 1);
        dy1  = out[0];

        xlat = areadata.lat[idy2][ixc];
        xlon = areadata.lon[idy2][ixc];
        out  = aodtv72_distance(xlat, xlon, xclat, xclon, 1);
        dy2  = out[0];

        dav  = (dx1 + dx2 + dy1 + dy2) / 4.0f;
        if (dav > 0.0) {
            rmw     = 2.8068f + (0.8361f * dav);  /* Howard's coeffs */
            eyesize = dav;
        }
        float[] outf = { rmw, eyesize };

        return outf;
    }


    /**
     *
     * @param inlat
     * @param inlon
     * @param searchtemp
     * @param searchtype
     * @param odtcurrent
     * @param areadata
     * @return
     */
static     float[] aodtv72_logspiral(float inlat, float inlon, float searchtemp,
                              int searchtype,
                              StormADOTInfo.IRData odtcurrent,
                              StormADOTInfo.DataGrid areadata)
    /* Determine storm location using 10^ Log-spiral analysis.
       Algorithm will attempt to match the spiral with the image
       pixels at or below the threshold temperature based on
       BD-enhancement curve values
        Inputs  : inlat      - center latitude of analysis grid
                  inlon      - center longitude of analysis grid
                  searchtemp - temperature threshold value
                  searchtype - 1=search at single point;2=search over 2^box
        Outputs : bestlat    - best latitude location from analysis
                  bestlon    - best longitude location from analysis
                  bestspiral - number of consecutive arcs through which spiral passes
    */
    {

        int   ixx, iyy, izz, iskip,
              rotfac = 0, theta, b;
        int   imaxx, iminx, imaxy, iminy, ycount;
        int   spiralconsec, maxconsec, spiralbest, spiralbestrot,
              bestrot = 0;
        float xrad    = 57.29578f,
              A       = 25.0f,
              B       = 10.0f / xrad;
        float maxx, minx, maxy, miny, xmindist;
        float xlat = 0.f,
              xlon = 0.f;
        float ylatdiff, ylondiff,
              xres = 6.0f;
        float thetax, r, thetaval, thetaval2;
        /* float zlat[bufsiz],zlon[bufsiz];*/
        float[] zlat, zlon, lres;
        int     np, ipt;
        int     bestspiral;
        float   bestlat = 0.f;
        float   bestlon = 0.f;


        if (odtcurrent.sattype == 5) {
            xres = 12.0f;
        }
        /* allocate memory */
        int nn = areadata.numx * areadata.numy;
        zlat = new float[nn];
        zlon = new float[nn];

        if (searchtype == 2) {
            /* search over 2.0 degree box */
            maxx  = inlat + 1.0f;
            minx  = inlat - 1.0f;
            maxy  = inlon + 1.0f;
            miny  = inlon - 1.0f;
            imaxx = (int) (maxx * 100.0);
            iminx = (int) (minx * 100.0);
            imaxy = (int) (maxy * 100.0);
            iminy = (int) (miny * 100.0);
        } else {
            /* search at a single point */
            imaxx = (int) (inlat * 100.0);
            iminx = (int) (inlat * 100.0);
            imaxy = (int) (inlon * 100.0);
            iminy = (int) (inlon * 100.0);
        }

        bestspiral = 0;

        /* initialize arrays */
        np = 0;
        for (ixx = 0; ixx < areadata.numx; ixx++) {
            for (iyy = 0; iyy < areadata.numy; iyy++) {
                if (areadata.temp[iyy][ixx] <= searchtemp) {
                    zlat[np] = areadata.lat[iyy][ixx];
                    zlon[np] = areadata.lon[iyy][ixx];
                    np++;
                }
            }
        }

        /* loop through x-axis/elements of analysis grid box */
        for (ixx = iminx; ixx <= imaxx; ixx = ixx + 20) {
            xlat = (float) ixx / 100.0f;
            /* loop through y-axis/lines of analysis grid box */
            for (iyy = iminy; iyy <= imaxy; iyy = iyy + 20) {
                xlon  = (float) iyy / 100.0f;
                iskip = 0;
                /* determine distance from each point in box to current location */
                if (searchtype == 2) {
                    xmindist = 12.0f;
                    for (izz = 0; izz < np; izz++) {
                        float[] out3 = aodtv72_distance(xlat, xlon,
                                           zlat[izz], zlon[izz], 1);
                        if (out3[0] <= xmindist) {
                            /* if the lat/lon point is too close to cold cloud tops, do
                               not calculate log spiral at this point.  Trying to eliminate
                               "false" arc locations by forcing the system to use some
                               of the arc points on the spiral away from the start of
                               the spiral (were getting "false echos" without this". */
                            iskip = 1;
                            break;
                        }
                    }
                }

                spiralbest    = 0;
                spiralbestrot = 0;
                /* if arc location passes analysis above, proceed with placement of spiral */
                if (iskip == 0) {
                    /* rotate the arc spiral thru entire revolution at 30 degree intervals */
                    for (rotfac = 0; rotfac <= 330; rotfac = rotfac + 30) {
                        spiralconsec = 0;
                        maxconsec    = 0;

                        /* calculate position of each point on spiral from 0 to 540^ */
                        for (theta = 0; theta <= 540; theta = theta + 15) {
                            thetax   = (float) theta / xrad;
                            r        = A * (float) Math.exp((B * thetax));
                            thetaval = (float) theta + (float) rotfac;
                            if (xlat < 0.0) {
                                thetaval = (float) (-1 * theta)
                                           + (float) rotfac;
                            }
                            thetaval2 = thetaval + 180.0f;
                            float[] out2 = aodtv72_distance2(xlat, xlon, r,
                                               thetaval2);
                            ycount = 0;
                            for (izz = 0; izz < np; izz++) {
                                ylatdiff = Math.abs(out2[0] - zlat[izz]);
                                ylondiff = Math.abs(out2[1] - zlon[izz]);
                                /* if a point is within 0.1^ latitude/longitude determine distance */
                                if ((ylatdiff <= 0.1) && (ylondiff <= 0.1)) {
                                    float[] out3 = aodtv72_distance(out2[0],
                                                       out2[1], zlat[izz],
                                                       zlon[izz], 1);
                                    /* if distance from spiral point is within 6km from an accepted
                                       temperature threshold point, count it */
                                    if (out3[0] <= xres) {
                                        ycount++;
                                    }
                                }
                            }
                            /* if there are 4 or more threshold points associated with each
                               spiral point, count within consecutive spiral point counter */
                            if (ycount >= 4) {
                                spiralconsec++;
                                /* save spiral that has the maximum consecutive spiral counts
                                   for each rotation though 360^ at each center location */
                                if (spiralconsec > maxconsec) {
                                    maxconsec = spiralconsec;
                                }
                            } else {
                                spiralconsec = 0;
                            }
                            /* if this spiral has the greatest number of consecutive spiral
                               points, save the location and number of points */
                            if (maxconsec > spiralbest) {
                                spiralbest    = maxconsec;
                                spiralbestrot = rotfac;
                            }
                        }
                    }  /* rotfac loop */
                    if (spiralbest > bestspiral) {
                        bestspiral = spiralbest;
                        bestlat    = xlat;
                        bestlon    = xlon;
                        bestrot    = spiralbestrot;
                    }
                }      /* iskip if */
            }          /* iyy loop */
        }              /* ixx loop */

        /* free memory */


        /* load array for best spiral band */
        ipt = 0;
        for (theta = 0; theta <= 540; theta = theta + 15) {
            thetax   = (float) theta / xrad;
            r        = A * (float) Math.exp((B * thetax));
            thetaval = (float) theta + (float) bestrot;
            if (xlat < 0.0) {
                thetaval = (float) (-1 * theta) + (float) rotfac;
            }
            thetaval2 = thetaval + 180.0f;
            float[] out2 = aodtv72_distance2(bestlat, bestlon, r, thetaval2);
            /* load array for external plotting of spiral band */
            //  spiralband_v72[0][ipt]=out2[0];
            // spiralband_v72[1][ipt]=out2[1];
            ipt++;
        }

        float[] out = { bestspiral, bestlat, bestlon };
        return out;


    }






    /**
     * _more_
     *
     * @param bin _more_
     * @param nbin _more_
     *
     * @return _more_
     */
static     float[] aodtv72_calcskew(float[] bin, int nbin)
    /* Calculate average, standard deviation, and skew for a given data set.
        Inputs  : bin  - data array
                  nbin - number of points in data array
        Outputs : ave  - average value in array
                  stdv - standard deviation of data array
                  skew - skew of data array histogram
    */
    {
        int   ixx;
        float xave, xstdv, xskew;
        float a2sum = 0.0f,
              a3sum = 0.0f,
              nsum  = 0.0f;
        float ave, stdv, skew;

        for (ixx = 0; ixx < nbin; ixx++) {
            nsum = nsum + bin[ixx];
        }
        /* compute average value of data array */
        xave = nsum / (float) nbin;

        for (ixx = 0; ixx < nbin; ixx++) {
            a2sum = a2sum + ((bin[ixx] - xave) * (bin[ixx] - xave));
            a3sum = a3sum
                    + ((bin[ixx] - xave) * (bin[ixx] - xave)
                       * (bin[ixx] - xave));
        }
        if (nbin <= 1) {
            xstdv = 0.0f;
            xskew = 0.0f;
        } else {
            /* calculate standard deviation of data array */
            xstdv = (float) Math.sqrt((1.0f / ((float) nbin - 1.0f)) * a2sum);
            /* calculate skew of data array */
            xskew = (float) ((1.0f / ((float) nbin - 1.0f)) * a3sum)
                    / (xstdv * xstdv * xstdv);
        }

        /* return desired values */
        ave  = xave;
        stdv = xstdv;
        skew = xskew;
        float[] out = { ave, stdv, skew };
        return out;
    }

    /**
     * _more_
     *
     * @param x1 _more_
     * @param x2 _more_
     * @param i2 _more_
     * @param ndim _more_
     */
static     void aodtv72_xxsort(float[] x1, float[] x2, float[] i2, int ndim) {
        int   ih, i;
        float x, top;

        ih  = aodtv72_fminx(x1);
        x   = x1[ih];
        top = x - 1.0f;
        for (i = 0; i < ndim; i++) {
            ih     = aodtv72_fmaxx(x1);
            i2[i]  = ih;
            x2[i]  = x1[ih];
            x1[ih] = top;
        }
    }

    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
static     int aodtv72_fminx(float[] f) {
        int   i;
        int   im;
        int   ndim = f.length;
        float x;

        im = 0;
        x  = f[0];
        for (i = 1; i < ndim; i++) {
            if (f[i] < x) {
                x  = f[i];
                im = i;
            }
        }
        return im;
    }

    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
static     int aodtv72_fmaxx(float[] f) {
        int   i;
        int   im;
        int   ndim = f.length;
        float x;

        im = 0;
        x  = f[0];
        for (i = 1; i < ndim; i++) {
            if (f[i] > x) {
                x  = f[i];
                im = i;
            }
        }
        return im;
    }

    /**
     * _more_
     *
     * @param cbd _more_
     *
     * @return _more_
     */
static     float[] aodtv72_fft(float[] cbd) {
        int      ixx, idxc, iok;
        double[] xr, xi, magn;
        double   dx, a, x;
        float    dx2;
        int      idxcnt;

        xr   = new double[64];
        xi   = new double[64];
        magn = new double[64];
        /* initialize arrays */
        for (ixx = 1; ixx <= 64; ++ixx) {
            xr[ixx - 1]   = cbd[ixx - 1];
            xi[ixx - 1]   = (float) 0.;
            magn[ixx - 1] = (float) 0.;
        }
        /* call FFT routine */
        iok = aodtv72_dfft(xr, xi, 64);
        if (iok <= 0) {
            return null;
        }
        for (ixx = 1; ixx <= 64; ++ixx) {
            magn[ixx - 1] = aodtv72_cmplx_abs(xr[ixx - 1], xi[ixx - 1]);
        }
        /* compute number of harmonics and magnitude of main harmonic */
        dx   = (float) 0.;
        idxc = 0;
        for (ixx = 2; ixx <= 31; ++ixx) {
            a  = magn[ixx - 2];
            x  = magn[ixx - 1];
            dx += (x + a) / (float) 2.;
            if ((magn[ixx - 1] > magn[ixx - 2])
                    && (magn[ixx - 1] > magn[ixx])) {
                ++idxc;
            }
        }
        dx2    = (float) (dx / magn[0]);

        idxcnt = idxc;

        float[] out = { dx2, idxcnt };

        return out;

    }  /* MAIN__ */



    /**
     * _more_
     *
     * @param real _more_
     * @param imag _more_
     *
     * @return _more_
     */
static     double aodtv72_cmplx_abs(double real, double imag) {
        double temp;
        if (real < 0) {
            real = -real;
        }
        if (imag < 0) {
            imag = -imag;
        }
        if (imag > real) {
            temp = real;
            real = imag;
            imag = temp;
        }
        if ((real + imag) == real) {
            return (real);
        }

        temp = imag / real;
        temp = real * Math.sqrt(1.0 + temp * temp);  /*overflow!!*/
        return (temp);

    }

    /*
           A Duhamel-Hollman split-radix dif fft
           Ref: Electronics Letters, Jan. 5, 1984
           Complex input and output data in arrays x and y
           Length is n.
    */

    /**
     * _more_
     *
     * @param x _more_
     * @param y _more_
     * @param np _more_
     *
     * @return _more_
     */
static     int aodtv72_dfft(double[] x, double[] y, int np) {

        int    i, j, k, m, n, i0, i1, i2, i3, is, id, n1, n2, n4;
        double a, e, a3, cc1, ss1, cc3, ss3, r1, r2, s1, s2, s3, xt;


        double px[] = new double[x.length+1];
        double py[] = new double[y.length+1];

        px[0] = 0;
        py[0] = 0;
        for (i = 0; i < x.length; i++) {
            px[i+1] = x[i];
            py[i+1] = y[i];
        }

        i = 2;
        m = 1;

        while (i < np) {
            i = i + i;
            m = m + 1;
        }
        ;
        n = i;
        if (n != np) {
            for (i = np + 1; i <= n; i++) {
                px[i] = 0.0;
                py[i] = 0.0;
            }
            /*printf("\nuse %d point fft",n);*/
        }

        n2 = n + n;
        for (k = 1; k <= m - 1; k++) {
            n2 = n2 / 2;
            n4 = n2 / 4;
            e  = 2.0 * PI / n2;
            a  = 0.0;
            for (j = 1; j <= n4; j++) {
                a3  = 3.0 * a;
                cc1 = Math.cos(a);
                ss1 = Math.sin(a);
                cc3 = Math.cos(a3);
                ss3 = Math.sin(a3);
                a   = j * e;
                is  = j;
                id  = 2 * n2;
                while (is < n) {
                    for (i0 = is; i0 <= n - 1; i0 = i0 + id) {
                        i1     = i0 + n4;
                        i2     = i1 + n4;
                        i3     = i2 + n4;
                        r1     = px[i0] - px[i2];
                        px[i0] = px[i0] + px[i2];
                        r2     = px[i1] - px[i3];
                        px[i1] = px[i1] + px[i3];
                        s1     = py[i0] - py[i2];
                        py[i0] = py[i0] + py[i2];
                        s2     = py[i1] - py[i3];
                        py[i1] = py[i1] + py[i3];
                        s3     = r1 - s2;
                        r1     = r1 + s2;
                        s2     = r2 - s1;
                        r2     = r2 + s1;
                        px[i2] = r1 * cc1 - s2 * ss1;
                        py[i2] = -s2 * cc1 - r1 * ss1;
                        px[i3] = s3 * cc3 + r2 * ss3;
                        py[i3] = r2 * cc3 - s3 * ss3;
                    }
                    is = 2 * id - n2 + j;
                    id = 4 * id;
                }
            }
        }

        /*
        ---------------------Last stage, length=2 butterfly---------------------
        */
        is = 1;
        id = 4;
        while (is < n) {
            for (i0 = is; i0 <= n; i0 = i0 + id) {
                i1     = i0 + 1;
                r1     = px[i0];
                px[i0] = r1 + px[i1];
                px[i1] = r1 - px[i1];
                r1     = py[i0];
                py[i0] = r1 + py[i1];
                py[i1] = r1 - py[i1];
            }
            is = 2 * id - 1;
            id = 4 * id;
        }

        /*
        c--------------------------Bit reverse counter
        */
        j  = 1;
        n1 = n - 1;
        for (i = 1; i <= n1; i++) {
            if (i < j) {
                xt    = px[j];
                px[j] = px[i];
                px[i] = xt;
                xt    = py[j];
                py[j] = py[i];
                py[i] = xt;
            }
            k = n / 2;
            while (k < j) {
                j = j - k;
                k = k / 2;
            }
            j = j + k;
        }

        /*
          for (i = 1; i<=16; i++) printf("%d  %g   %gn",i,*(px+i),(py+i));
        */
       for (i = 0; i < x.length -1; i++) {
            x[i] = px[i+1];
            y[i] = py[i+1];
        }
        return (n);
    }



    /**
     * _more_
     *
     * @param keyer _more_
     * @param areadata _more_
     *
     * @return _more_
     */
static     public StormADOTInfo.IRData aodtv72_gettemps(int keyer,
            StormADOTInfo.DataGrid areadata)
    /* Determine eye and coldest-warmest cloud temperatures.
        Inputs  : keyer      - eye radius (km)
        Outputs : odtcurrent_v72 - structure containing current image information
        Return  : -51 : eye, cloud, or warmest temperature <-100C or >+40C
    */
    {

        int                  cursorx, cursory;
        int                  iok, idist, iret;
        float                cursortemp, eyet, cloudt;
        float cursorlat, cursorlon, warmlat, warmlon, eyeangle, eyedist;
        StormADOTInfo        sinfo      = new StormADOTInfo();
        StormADOTInfo.IRData odtcurrent = new StormADOTInfo.IRData();

        /* calculate cursorx/cursory from numx/numy... values should be 0.5*numx/y */
        cursorx    = areadata.numx / 2;
        cursory    = areadata.numy / 2;
        cursortemp = areadata.temp[cursory][cursorx];
        cursorlat  = areadata.lat[cursory][cursorx];
        cursorlon  = areadata.lon[cursory][cursorx];

        // tcircfirst_v72=(struct ringdata *)calloc(1,sizeof(struct ringdata));

        /* load array containing data on rings around center location */
        List<StormADOTInfo.RingData> tcircfirst = aodtv72_readcirc(cursorx,
                                                      cursory, areadata);

        iret = 0;
        /* compute eye/warmest pixel temperature */
        StormADOTInfo.RingData eye = aodtv72_calceyetemp(keyer, cursortemp,
                                         tcircfirst);
        eyet = eye.temp;
        if ((eyet < -100.0) || (eyet > 40.0)) {
            iret = -51;
        }

        if (keyer == StormADOTInfo.keyerA_v72) {
            /* set warmest pixel temperature */
            odtcurrent.warmt = eyet;
            /* calculate warmest pixel location */
            float[] out2 = aodtv72_distance2(cursorlat, cursorlon, eye.dist,
                                             eye.angle);

            odtcurrent.warmlatitude  = out2[0];  //warmlat;
            odtcurrent.warmlongitude = out2[1];  //warmlon;
            /* store forecast location temperature in eyet */
            odtcurrent.eyet = cursortemp - 273.16f;
            //free(tcircfirst_v72);
        } else {
            /* set eye temperature */
            odtcurrent.eyet = eyet;
            /* set cloud temperature and ring distance */
            float[] out3 = aodtv72_calccloudtemp(tcircfirst);
            cloudt = out3[0];
            idist  = (int) out3[1];
            if ((eyet < -100.0) || (eyet > 40.0)) {
                iret = -51;
            }
            odtcurrent.cwcloudt = cloudt;
            odtcurrent.cwring   = idist;
        }
        if(iret == -51) return null;
        return odtcurrent;
    }

    /**
     * _more_
     *
     * @param keyer _more_
     * @param cursortemp _more_
     * @param tcircfirst _more_
     *
     * @return _more_
     */
static     public StormADOTInfo.RingData aodtv72_calceyetemp(int keyer,
            float cursortemp, List<StormADOTInfo.RingData> tcircfirst)
    /* Determine eye/warmest temperature by examining the satellite
       data between 0 and 24/75 km from the storm center location.
       Eye temperature will be warmest temperature found.
        Inputs  : keyer      - analysis region radius
                  cursortemp - temperature of pixel at cursor location
        Outputs : eyeangle   - angle to warmest temperature in eye region (if found)
                  eyedist    - distance to warmest temperature in eye region (if found)
        Return  : return value is warmest eye temperature
    */
    {

        List<StormADOTInfo.RingData> tcirc;
        StormADOTInfo.RingData       eye = new StormADOTInfo.RingData();
        /* set eye temp to cursor location temperature */
        eye.temp  = cursortemp;

        eye.dist  = 0.0f;
        eye.angle = 0.0f;
        tcirc     = tcircfirst;
        Iterator it = tcirc.iterator();
        while (it.hasNext()) {
            StormADOTInfo.RingData rd = (StormADOTInfo.RingData) it.next();
            if (rd.dist <= (float) keyer) {
                if (rd.temp > eye.temp) {
                    eye.temp  = rd.temp;
                    eye.dist  = rd.dist;
                    eye.angle = rd.angle;
                }
            }

        }

        /* convert temperature to C from K */
        eye.temp = (eye.temp - 273.16f);

        return eye;
    }

    /**
     * _more_
     *
     * @param tcircfirst _more_
     *
     * @return _more_
     */
static     public float[] aodtv72_calccloudtemp(
            List<StormADOTInfo.RingData> tcircfirst)
    /* Determine surrounding cloud top region temperature by
       examining the satellite data between kstart_v72(24 km) and
       kend_v72 (136 km) from the storm center location.  Cloud
       temperature will be the coldest value of an array
       of warmest ring temperatures (4 km ring sizes for 4 km
       infrared data).  This is the "coldest-warmest" pixel.
        Inputs  : none
        Outputs : irdist - distance (km) from center to cloud top
                           temperature value (distance to ring)
                  return value is cloud top temperature value
    */
    {
        int iyy, b;
        int numrings = (StormADOTInfo.kend_v72 - StormADOTInfo.kstart_v72)
                       / StormADOTInfo.kres_v72;
        int kring;
        /* float maxtemp[200]; */
        float[]                      maxtemp;

        List<StormADOTInfo.RingData> tcirc;

        float                        cloudtemp = 10000.0f;

        float[]                      out       = new float[2];
        int                          irdist    = 0;
        /* initialize maxtemp array */
        maxtemp = new float[numrings];

        tcirc   = tcircfirst;
        Iterator it = tcirc.iterator();
        while (it.hasNext()) {
            StormADOTInfo.RingData rd = (StormADOTInfo.RingData) it.next();
            if ((rd.dist >= (float) StormADOTInfo.kstart_v72)
                    && (rd.dist < (float) StormADOTInfo.kend_v72)) {
                kring = ((int) rd.dist - StormADOTInfo.kstart_v72)
                        / StormADOTInfo.kres_v72;
                if (rd.temp > maxtemp[kring]) {
                    maxtemp[kring] = rd.temp;
                }
            }
        }

        /* search maxtemp array for coldest temperature */
        for (iyy = 0; iyy < numrings; iyy++) {
            if ((maxtemp[iyy] < cloudtemp) && (maxtemp[iyy] > 160.0)) {
                cloudtemp = maxtemp[iyy];
                irdist = (iyy * ucar.unidata.data.storm.StormADOTInfo.kres_v72)
                         + StormADOTInfo.kstart_v72;
            }
        }

        cloudtemp = (cloudtemp - 273.16f);
        out[0]    = cloudtemp;
        out[1]    = irdist;
        return out;
    }

    /**
     * _more_
     *
     * @param ixc _more_
     * @param iyc _more_
     * @param areadata _more_
     *
     * @return _more_
     */
static     public List<StormADOTInfo.RingData> aodtv72_readcirc(int ixc, int iyc,
            StormADOTInfo.DataGrid areadata)
    /* Read array of satellite data temperatures and load array
       containing temperatures and lat/lon positions.
        Inputs  : ixc   - element location of center point
                  iyc   - line location of center point
        Outputs : global structure tcirc containing temperature/locations
    */
    {
        int                          ixx, iyy, b,
                                     count = 0;
        float                        xclat, xclon, xlat, xlon, xdist, xangle;
        StormADOTInfo                sinfo      = new StormADOTInfo();
        StormADOTInfo.RingData       tcirc      = null;

        List<StormADOTInfo.RingData> tcircfirst = new ArrayList();

        /* obtain center/cursor x,y position */
        xclat = areadata.lat[iyc][ixc];
        xclon = areadata.lon[iyc][ixc];
        /* load tcirc array with distance, angle, and temp info */

        for (iyy = 0; iyy < areadata.numy; iyy++) {
            for (ixx = 0; ixx < areadata.numx; ixx++) {
                xlat = areadata.lat[iyy][ixx];
                xlon = areadata.lon[iyy][ixx];
                float[] outDA = aodtv72_distance(xlat, xlon, xclat, xclon, 1);
                xdist  = outDA[0];
                xangle = outDA[1];
                if (xdist <= (float) (StormADOTInfo.kend_v72 + 80)) {  /* add 80.0 to allow for correct calculation of annulus temp */
                    count++;
                    tcirc = new StormADOTInfo.RingData(xdist, xangle,
                            areadata.temp[iyy][ixx]);
                    tcircfirst.add(tcirc);
                    // tcirc.nextrec=NULL; /* make pointer for last record equal to 0 */
                }
            }
        }

        return tcircfirst;
    }


    /**
     * _more_
     *
     * @param rrlat _more_
     * @param rrlon _more_
     * @param pplat _more_
     * @param pplon _more_
     * @param iunit _more_
     *
     * @return _more_
     */
static     float[] aodtv72_distance(float rrlat, float rrlon, float pplat,
                             float pplon, int iunit)
    /* Calculate distance and angle between two points
       (rrlat,rrlon and pplat,pplon).
       Inputs  : rrlat - latitude of starting point
                 rrlon - longitude of starting point
                 rrlat - latitude of ending point
                 rrlon - longitude of ending point
                 iunit - flag for output unit type (1-km,2-mi,3-nmi)
       Outputs : dist  - distance between two points
                 ang   - angle between two points (0=north)
    */
    {
        float   z = 0.017453292f,
                r = 6371.0f;
        float   rlat, rlon, plat, plon;
        float   crlat, crlon, srlat, srlon;
        float   cplat, cplon, splat, splon;
        float   aa, xx, yy, zz, idist, xdist, xang;
        float[] out = new float[2];
        float   dist;
        float   ang;

        idist = 0.0f;
        rlat  = rrlat * z;
        rlon  = rrlon * z;
        plat  = pplat * z;
        plon  = pplon * z;
        crlat = (float) Math.cos(rlat);
        crlon = (float) Math.cos(rlon);
        srlat = (float) Math.sin(rlat);
        srlon = (float) Math.sin(rlon);
        cplat = (float) Math.cos(plat);
        cplon = (float) Math.cos(plon);
        splat = (float) Math.sin(plat);
        splon = (float) Math.sin(plon);
        xx    = (cplat * cplon) - (crlat * crlon);
        yy    = (cplat * splon) - (crlat * srlon);
        zz    = splat - srlat;
        aa    = (xx * xx) + (yy * yy) + (zz * zz);
        idist = (float) Math.sqrt(aa);
        /* xdist is distance in kilometers */
        xdist = 2.0f * (float) Math.asin(idist / 2.0f) * r;

        if (iunit == 2) {
            xdist = ((69.0f * xdist) + 55f) / 111.0f;  /* conversion to miles */
        }
        if (iunit == 3) {
            xdist = ((60.0f * xdist) + 55f) / 111.0f;  /* conversion to nautical miles */
        }
        dist = xdist;

        xang = 0.0f;
        if (Math.abs(xdist) > 0.0001) {
            xang = (float) ((Math.sin(rlon - plon)
                             * Math.sin((3.14159 / 2.0)
                                        - plat)) / Math.sin(idist));
        }
        if (Math.abs(xang) > 1.0) {
            xang = (xang >= 0)
                   ? 1
                   : -1;
        }
        xang = (float) Math.asin(xang) / z;
        if (plat < rlat) {
            xang = 180.0f - xang;
        }
        if (xang < 0.0) {
            xang = 360.0f + xang;
        }
        ang    = xang;
        out[0] = dist;
        out[1] = ang;

        return out;
    }

    /**
     * _more_
     *
     * @param rlat _more_
     * @param rlon _more_
     * @param xdist _more_
     * @param xang _more_
     *
     * @return _more_
     */
static     float[] aodtv72_distance2(float rlat, float rlon, float xdist, float xang)
    /* Calculate a latitude and longitude position from an
       initial latitude/longitude and distance/angle values.
        Inputs  : rlat - initial latitude
                  rlon - initial longitude
                  dist - distance from initial position
                  ang  - angle from initial position
        Outputs : plat - derived latitude
                  plon - derived longitude
    */
    {
        float   z   = 0.017453292f,
                z90 = 1.570797f;
        float   clat, clon, cltv, cdis, cang;
        float   qlat, qlon, argu, tv;
        int     iang;
        float   plat, plon;
        float[] out = new float[2];

        clat = (90.0f - rlat) * z;
        cltv = clat;
        clon = rlon * z;
        if (rlat < 0.0) {
            cltv = -(90.0f + rlat) * z;
            clon = (rlon - 180.0f) * z;
            xang = 360 - xang;
        }
        iang = (int) xang;
        cang = -(float) (((540 - iang) % 360) * z);
        cdis = (xdist / 111.1f) * z;
        qlat = (float) Math.acos((Math.cos(clat) * Math.cos(cdis))
                                 + (Math.sin(clat) * Math.sin(cdis)
                                    * Math.cos(cang)));
        if (Math.abs(qlat) < 0.0000001) {
            qlon = 0.0f;
        } else {
            argu = (float) (Math.sin(cdis) * Math.sin(cang))
                   / (float) Math.sin(qlat);
            if (Math.abs(argu) > 1.0) {
                argu = (argu >= 0)
                       ? 1
                       : -1;;
            }
            qlon = (float) Math.asin(argu);
            tv = (float) Math.atan(Math.sin(z90 - cang))
                 / (float) Math.tan(z90 - cdis);
            if (tv > cltv) {
                qlon = (2.0f * z90) - qlon;
            }
        }
        qlon = clon - qlon;
        plat = 90.0f - (qlat / z);
        plon = (float) ((int) (10000 * (qlon / z)) % 3600000) / 10000.0f;
        if (plon < -180) {
            plon = plon + 360;
        }

        out[0] = plat;
        out[1] = plon;

        return out;
    }

    /**
     * _more_
     *
     * @param date _more_
     * @param time _more_
     *
     * @return _more_
     */
static     double aodtv72_calctime(int date, int time)
    /* Compute time in xxxxx.yyy units, where xxxxx is the
       day and yyy is the percentage of the day.  This routine
       will also correct for Y2K problems.
        Inputs  : date - Julian date
                  time - time in HHMMSS format
        Outputs : function return value
    */
    {
        int    iyy;
        float  sec, min, hour, partday;
        double timeout;

        if ((date % 1000) == 0) {
            return 0;
        }
        if (time < 0) {
            return 0;
        }

        iyy = date / 1000;  /* obtain year */
        /* check for millenium designation in the year.
           if it is not there, add it onto the beginning */
        if (iyy < 1900) {
            if (iyy > 70) {
                date = 1900000 + date;
            } else {
                date = 2000000 + date;
            }
        }

        sec     = ((float) (time % 100)) / 3600.0f;
        min     = ((float) ((time / 100) % 100)) / 60.0f;
        hour    = (float) (time / 10000);
        partday = (hour + min + sec) / 24.0f;
        timeout = (double) date + (double) partday;

        return timeout;
    }

    /**
     *
     * @param xlat
     * @param xlon
     * @param tempval
     * @param atype
     * @param areadata _more_
     * @return
     */
static     float[] aodtv72_cdoshearcalc(float xlat, float xlon, float tempval,
                                 int atype, StormADOTInfo.DataGrid areadata)
    /* Determine eye size or shear distance for a given scene.
        Inputs  : xlat    - center latitude of analysis grid
                  xlon    - center longitude of analysis grid
                  tempval - temperature threshold value to be used
                  atype   - analysis type (1-cdo size,2-eye size,3-shear distance)
        Outputs : valb    - eye/cdo radius or shear distance
                  valc    - eye/cdo symmetry value or 0
    */
    {

        int     ixx, iyy, np, b,
                numvalid = 0;
        float   smalldist, vc, maxdist;
        float[] zlat, zlon;
        float   a1, a2, a3, a4, v1, v2, v3;
        float   valb = 0.f,
                valc = 0.f;
        /* allocate memory */

        np = 0;
        int nn = areadata.numx * areadata.numy;
        zlat = new float[nn];
        zlon = new float[nn];
        /* CDO size determination - RETURNS RADIUS */
        if (atype == 1) {
            /* a1=999.9;a2=999.9;a3=999.9;a4=999.9; */
            a1 = 300.0f;
            a2 = 300.0f;
            a3 = 300.0f;
            a4 = 300.0f;
            for (ixx = 0; ixx < areadata.numx; ixx++) {
                for (iyy = 0; iyy < areadata.numy; iyy++) {
                    if (areadata.temp[iyy][ixx] > tempval) {
                        zlat[np] = areadata.lat[iyy][ixx];
                        zlon[np] = areadata.lon[iyy][ixx];
                        np++;
                    }
                }
            }
            /* printf("np=%d  numx*numy=%d\n",np,areadata_v72->numx*areadata_v72->numy); */
            maxdist = 0.0f;
            if (np < (areadata.numx * areadata.numy)) {
                for (ixx = 0; ixx < np; ixx++) {
                    float[] out = aodtv72_distance(xlat, xlon, zlat[ixx],
                                      zlon[ixx], 1);
                    if (out[0] > maxdist) {
                        maxdist = out[0];
                    }
                    /* determine size of CDO */
                    if (out[0] > StormADOTInfo.keyerM_v72) {
                        if ((Math.abs(out[1] - 45.0) <= 15.0)
                                && (out[0] < a1)) {
                            a1 = out[0];
                        }
                        if ((Math.abs(out[1] - 135.0) <= 15.0)
                                && (out[0] < a2)) {
                            a2 = out[0];
                        }
                        if ((Math.abs(out[1] - 225.0) <= 15.0)
                                && (out[0] < a3)) {
                            a3 = out[0];
                        }
                        if ((Math.abs(out[1] - 315.0) <= 15.0)
                                && (out[0] < a4)) {
                            a4 = out[0];
                        }
                    }
                }
                /* printf("a1=%f a2=%f a3=%f a4=%f\n",a1,a2,a3,a4); */
                numvalid = 4;
                v3       = StormADOTInfo.keyerM_v72 + StormADOTInfo.kres_v72;
                if (a1 < v3) {
                    numvalid--;
                }
                if (a2 < v3) {
                    numvalid--;
                }
                if (a3 < v3) {
                    numvalid--;
                }
                if (a4 < v3) {
                    numvalid--;
                }
            } else {
                a1 = 0.0f;
                a2 = 0.0f;
                a3 = 0.0f;
                a4 = 0.0f;
            }
            if (numvalid < 3) {
                valb = 0.0f;
                valc = 0.0f;
            } else {
                a1   = Math.min(a1, maxdist);
                a2   = Math.min(a2, maxdist);
                a3   = Math.min(a3, maxdist);
                a4   = Math.min(a4, maxdist);
                valb = (a1 + a2 + a3 + a4) / 4.0f;
                /* *valc=A_ABS(((a1+a3)/2.0)-((a2+a4)/2.0))/2.0; */
                v1   = a1 + a3;
                v2   = a2 + a4;
                vc   = v1 / v2;
                vc   = Math.max(vc, 1.0f / vc);
                valc = vc;
            }
            /* printf("\nnp=%5.5d  a1=%5.1f a2=%5.1f a3=%5.1f a4=%5.1f  ",np,a1,a2,a3,a4); */
        }

        /* shear distance determination */
        if (atype == 3) {
            a1 = 999.9f;
            a2 = 999.9f;
            a3 = 999.9f;
            a4 = 999.9f;
            for (ixx = 0; ixx < areadata.numx; ixx++) {
                for (iyy = 0; iyy < areadata.numy; iyy++) {
                    if (areadata.temp[iyy][ixx] <= tempval) {
                        zlat[np] = areadata.lat[iyy][ixx];
                        zlon[np] = areadata.lon[iyy][ixx];
                        np++;
                    }
                }
            }
            smalldist = 999.9f;
            for (ixx = 0; ixx < np; ixx++) {
                float[] out = aodtv72_distance(xlat, xlon, zlat[ixx],
                                  zlon[ixx], 1);
                if (out[0] < smalldist) {
                    smalldist = out[0];
                }
            }
            valb = smalldist;
            valc = 0.0f;


        }

        /* free memory */

        float[] out = { valb, valc };
        return out;

    }


}

