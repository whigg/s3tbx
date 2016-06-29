/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.gaseousabsorption;

import com.bc.ceres.core.ProgressMonitor;
import com.google.common.primitives.Doubles;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.CsvReader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author muhammad.bc.
 */
public class GaseousAbsorptionAuxII {

    private List<double[]> ozoneHighs;


    private List<double[]> coeffhighres = new ArrayList();

    public GaseousAbsorptionAuxII() {
        try {
            Path installAuxdata = installAuxdata();
            Path resolve = installAuxdata.resolve("ozone-highres.txt");
            FileReader fileReader = new FileReader(resolve.toString());
            CsvReader reader = new CsvReader(fileReader, new char[]{' ', '\t'});
            ozoneHighs = reader.readDoubleRecords();
            coeffhighres = getCoeffhighres(ozoneHighs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<double[]> getOzoneHighs() {
        return ozoneHighs;
    }

    public List<double[]> getCoeffhighres(List<double[]> ozoneHighs) {
        List<Double> O3wavelengthList = new ArrayList<>();
        List<Double> O3absorptionList = new ArrayList<>();

        for (double[] ozoneHigh : ozoneHighs) {
            O3wavelengthList.add(ozoneHigh[0]);
            O3absorptionList.add(ozoneHigh[1]);
        }
        List<double[]> O3Main = new ArrayList<>();

        O3Main.add(Doubles.toArray(O3wavelengthList));
        O3Main.add(Doubles.toArray(O3absorptionList));
        return O3Main;
    }

    Path installAuxdata() throws IOException {
        Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("olci/smile-correction");
        final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(GaseousAbsorptionAuxII.class).resolve("auxdata/smile");
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);
        resourceInstaller.install(".*", ProgressMonitor.NULL);
        return auxdataDirectory;
    }


    public double convolve(double lower, double upper) {
        double[] O3absorption = coeffhighres.get(1);
        double[] O3wavelength = coeffhighres.get(0);
        int length = O3absorption.length;
        int weight = 0;
        for (int i = 0; i < length; i++) {
            if (O3wavelength[i] >= lower && O3wavelength[i] <= upper) {
                weight += 1;
            }
        }
        double O3value = Arrays.stream(O3absorption).sum() / weight;
        return O3value;
    }


    public double[] absorptionOzone(String instrument) {
        List<Number> o3absorpInstrument = new ArrayList<>();

        double[] lamC;
        if (instrument.equals("MERIS")) {
            double[] absorb_ozon = new double[]{0.0002174, 0.0034448, 0.0205669, 0.0400134, 0.105446, 0.1081787, 0.0501634, 0.0349671, 0.0187495, 0.0086322, 0.0000001, 0.0084989, 0.0018944, 0.0012369, 0.000001};
            lamC = new double[]{412.5, 442.0, 490.0, 510.0, 560.0, 620.0, 665.0, 681.25, 708.75, 753.0, 761.25, 779.0, 865.0,
                    885.0, 900};
            double[] lamW = new double[]{10., 10., 10., 10., 10., 10., 10., 10., 10., 10., 3.75, 10., 20., 20., 40.};
            for (int i = 0; i < lamC.length; i++) {
                double lower = lamC[i] - lamW[i] / 2;
                double upper = lamC[i] + lamW[i] / 2;
                o3absorpInstrument.add(convolve(lower, upper));
            }
        }
        if (instrument.equals("OLCI")) {
            lamC = new double[]{
                    400.0, 412.5, 442.0, 490.0, 510.0, 560.0, 620.0, 665.0, 673.75, 681.25, 708.75, 753.75, 761.25, 764.375, 767.5, 778.75, 865.0,
                    885.0, 900.0, 940.0, 1020.0};
            double[] lamW = new double[]{15., 10., 10., 10., 10., 10., 10., 10., 7.5, 7.5, 10., 7.5, 2.5, 3.75, 2.5, 15., 20., 10., 10., 20., 40.};

            for (int i = 0; i < lamC.length; i++) {
                double lower = lamC[i] - lamW[i] / 2;
                double upper = lamC[i] + lamW[i] / 2;
                o3absorpInstrument.add(convolve(lower, upper));
            }
        }
        return Doubles.toArray(o3absorpInstrument);
    }
}