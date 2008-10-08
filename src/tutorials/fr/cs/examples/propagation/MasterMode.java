/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

package fr.cs.examples.propagation;

import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.CunninghamAttractionModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.UTCScale;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for master mode propagation.
 * <p>This tutorial shows the interest of the master mode which hides the complex
 * internal mechanic of the propagation and just fulfills the user main needs.<p>
 * @author Fabien Maussion
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class MasterMode {

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();

            // gravitation coefficient
            double mu =  3.986004415e+14;

            // inertial frame
            Frame inertialFrame = Frame.getEME2000();

            // Initial date
            AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000,
                                                        UTCScale.getInstance());

            // Initial orbit
            double a = 24396159; // semi major axis in meters
            double e = 0.72831215; // eccentricity
            double i = Math.toRadians(7); // inclination
            double omega = Math.toRadians(180); // perigee argument
            double raan = Math.toRadians(261); // right ascention of ascending node
            double lv = 0; // mean anomaly
            Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lv,
                                                    KeplerianOrbit.MEAN_ANOMALY, 
                                                    inertialFrame, initialDate, mu);

            // Initial state definition
            SpacecraftState initialState = new SpacecraftState(initialOrbit);

            // Adaptive step integrator with a minimum step of 0.001 and a maximum step of 1000
            final double minStep = 0.001;
            final double maxstep = 1000.0;
            final double[] absoluteTolerance = {
                0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
            };
            final double[] relativeTolerance = {
                1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
            };
            AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxstep,
                                                                                   absoluteTolerance,
                                                                                   relativeTolerance);

            // Propagator
            NumericalPropagator propagator = new NumericalPropagator(integrator);

            // Force Model (reduced to perturbing gravity field)
            Frame ITRF2005 = Frame.getITRF2005(); // terrestrial frame at an arbitrary date
            double ae  =  6378137.; // equatorial radius in meter
            double c20 = -1.08262631303e-3; // J2 potential coefficient
            double[][] c = new double[3][1];
            c[0][0] = 0.0;
            c[2][0] = c20;
            double[][] s = new double[3][1]; // potential coefficients arrays (only J2 is considered here)
            ForceModel cunningham = new CunninghamAttractionModel(ITRF2005, ae, mu, c, s);

            // Add force model to the propagator
            propagator.addForceModel(cunningham);

            // Set up initial state in the propagator
            propagator.setInitialState(initialState);

            // Set up operating mode for the propagator as master mode
            // with fixed step and specialized step handler
            propagator.setMasterMode(60., new TutorialStepHandler());

            // Extrapolate from the initial to the final date
            SpacecraftState finalState = propagator.propagate(new AbsoluteDate(initialDate, 630.));
            System.out.println(" Final date  : " + finalState.getDate());
            System.out.println(" Final state : " + finalState.getOrbit());
            
        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }
    
    /** Specialized step handler.
     * <p>This class extends the step handler in order to print on the output stream at the given step.<p>
     * @author Pascal Parraud
     */
    private static class TutorialStepHandler implements OrekitFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = -8909135870522456848L;

        private TutorialStepHandler() {
            //private constructor
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            System.out.println(" time : " + currentState.getDate());
            System.out.println(" " + currentState.getOrbit());
            if (isLast) {
                System.out.println(" this was the last step ");
            }

        }

    }

}