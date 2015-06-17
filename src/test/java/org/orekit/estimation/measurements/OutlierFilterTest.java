/* Copyright 2002-2015 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.estimation.measurements;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.FastMath;
import org.junit.Before;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.gravity.potential.AstronomicalAmplitudeReader;
import org.orekit.forces.gravity.potential.FESCHatEpsilonReader;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.OceanLoadDeformationCoefficients;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class OutlierFilterTest {

    private IERSConventions                      conventions;
    private OneAxisEllipsoid                     earth;
    private CelestialBody                        sun;
    private CelestialBody                        moon;
    private SphericalSpacecraft                  spacecraft;
    private NormalizedSphericalHarmonicsProvider gravity;
    private TimeScale                            utc;
    private UT1Scale                             ut1;
    private Orbit                                initialOrbit;
    private List<GroundStation>                  stations;

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential:tides");
        conventions = IERSConventions.IERS_2010;
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(conventions, true));
        sun = CelestialBodyFactory.getSun();
        moon = CelestialBodyFactory.getMoon();
        spacecraft = new SphericalSpacecraft(2.0, 1.2, 0.2, 0.8);
        utc = TimeScalesFactory.getUTC();
        ut1 = TimeScalesFactory.getUT1(conventions, true);
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        AstronomicalAmplitudeReader aaReader =
                        new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataProvidersManager.getInstance().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));
        gravity = GravityFieldFactory.getNormalizedProvider(20, 20);
        initialOrbit = new KeplerianOrbit(15000000.0, 0.125, 1.25,
                                          0.250, 1.375, 0.0625, PositionAngle.TRUE,
                                          FramesFactory.getEME2000(),
                                          new AbsoluteDate(2000, 2, 24, 11, 35,47.0, utc),
                                          gravity.getMu());

        stations = Arrays.asList(//createStation(-18.59146, -173.98363,   76.0, "Leimatu`a"),
                                 createStation(-53.05388,  -75.01551, 1750.0, "Isla Desolación"),
                                 createStation( 62.29639,   -7.01250,  880.0, "Slættaratindur")
                                 //createStation( -4.01583,  103.12833, 3173.0, "Gunung Dempo")
                                 );

    }

    private GroundStation createStation(double latitudeInDegrees, double longitudeInDegrees,
                                        double altitude, String name)
        throws OrekitException {
        final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(latitudeInDegrees),
                                                   FastMath.toRadians(longitudeInDegrees),
                                                   0.0);
        return new GroundStation(new TopocentricFrame(earth, gp, name));
    }

}


