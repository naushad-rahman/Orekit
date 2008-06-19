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
package org.orekit.forces.maneuvers;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.OrekitSwitchingFunction;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;


/** This class implements a simple maneuver with constant thrust.
 *
 * @author Fabien Maussion
 * @author Véronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class ConstantThrustManeuver implements ForceModel {

    /** Identifier for QSW frame. */
    public static final int QSW = 0;

    /** Identifier for TNW frame. */
    public static final int TNW = 1;

    /** Identifier for inertial frame. */
    public static final int INERTIAL = 2;

    /** Identifier for spacecraft frame. */
    public static final int SPACECRAFT = 3;

    /** Reference gravity acceleration constant (m/s<sup>2</sup>). */
    public static final double G0 = 9.80665;

    /** Serializable UID. */
    private static final long serialVersionUID = 5349622732741384211L;

    /** State of the engine. */
    private boolean firing;

    /** Frame type. */
    private final int frameType;

    /** Start of the maneuver. */
    private final AbsoluteDate startDate;

    /** End of the maneuver. */
    private final AbsoluteDate endDate;

    /** Duration (s). */
    private final double duration;

    /** Engine thrust. */
    private final double thrust;

    /** Engine flow-rate. */
    private final double flowRate;

    /** Direction of the acceleration in selected frame. */
    private final Vector3D direction;

    /** Simple constructor for a constant direction and constant thrust.
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative,
     * the date is considered to be the stop date)
     * @param thrust the thrust force (N)
     * @param isp the Isp (s)
     * @param direction the acceleration direction in chosen frame.
     * @param frameType the frame in which the direction is defined
     * @exception IllegalArgumentException if frame type is not one of
     * {@link #TNW}, {@link #QSW}, {@link #INERTIAL} or {@link #SPACECRAFT}
     * @see #QSW
     * @see #TNW
     * @see #INERTIAL
     */
    public ConstantThrustManeuver(final AbsoluteDate date, final double duration,
                                  final double thrust, final double isp,
                                  final Vector3D direction, final int frameType)
        throws IllegalArgumentException {

        if ((frameType != QSW) && (frameType != TNW) &&
            (frameType != INERTIAL) && (frameType != SPACECRAFT)) {
            throw OrekitException.createIllegalArgumentException("unsupported thrust direction frame, " +
                                                                 "supported types: {0}, {1}, {2} and {3}",
                                                                 new Object[] {
                                                                     "QSW", "TNW", "INERTIAL", "SPACECRAFT"
                                                                 });
        }

        if (duration >= 0) {
            this.startDate = date;
            this.endDate   = new AbsoluteDate(date, duration);
            this.duration  = duration;
        } else {
            this.endDate   = date;
            this.startDate = new AbsoluteDate(endDate, duration);
            this.duration  = -duration;
        }

        this.thrust    = thrust;
        this.flowRate  = -thrust / (G0 * isp);
        this.direction = direction.normalize();
        this.frameType = frameType;
        firing = false;

    }

    /** Compute the contribution of maneuver to the global acceleration.
     * @param s the current state information : date, cinematics, attitude
     * @param adder object where the contribution should be added
     * @exception OrekitException if some specific error occurs
     */
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
        throws OrekitException {

        if (firing) {

            // add thrust acceleration
            final double acceleration = thrust / s.getMass();
            switch (frameType) {
            case QSW :
                adder.addQSWAcceleration(acceleration * direction.getX(),
                                         acceleration * direction.getY(),
                                         acceleration * direction.getZ());
                break;
            case TNW :
                adder.addTNWAcceleration(acceleration * direction.getX(),
                                         acceleration * direction.getY(),
                                         acceleration * direction.getZ());
                break;
            case INERTIAL :
                adder.addXYZAcceleration(acceleration * direction.getX(),
                                         acceleration * direction.getY(),
                                         acceleration * direction.getZ());
                break;
            default :
                // the thrust is in spacecraft frame, it depends on attitude
                final Vector3D inertialThrust = s.getAttitude().getRotation().applyTo(direction);
                adder.addXYZAcceleration(acceleration * inertialThrust.getX(),
                                         acceleration * inertialThrust.getY(),
                                         acceleration * inertialThrust.getZ());
            }

            // add flow rate
            adder.addMassDerivative(flowRate);

        }

    }

    /** Gets the switching functions related to start and stop passes.
     * @return start / stop switching functions
     */
    public OrekitSwitchingFunction[] getSwitchingFunctions() {
        return new OrekitSwitchingFunction[] {
            new StartSwitch(), new StopSwitch()
        };
    }

    /** This class defines the beginning of the acceleration switching function.
     * It triggers at the ignition.
     */
    private class StartSwitch implements OrekitSwitchingFunction {

        /** Serializable UID. */
        private static final long serialVersionUID = 8256737374206837853L;

        /** {@inheritDoc} */
        public int eventOccurred(final SpacecraftState s) {
            // start the maneuver
            firing = true;
            return RESET_DERIVATIVES;
        }

        /** The G-function is the difference between the start date and the current date.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         */
        public double g(final SpacecraftState s) {
            return startDate.minus(s.getDate());
        }

        /** {@inheritDoc} */
        public double getMaxCheckInterval() {
            return duration;
        }

        /** {@inheritDoc} */
        public double getThreshold() {
            // convergence threshold in seconds
            return 1.0e-4;
        }

        /** {@inheritDoc} */
        public int getMaxIterationCount() {
            return 10;
        }

        /** {@inheritDoc} */
        public SpacecraftState resetState(final SpacecraftState oldState)
            throws OrekitException {
            // never called since eventOccurred does never return RESET_STATE
            return null;
        }

    }

    /** This class defines the end of the acceleration switching function.
     * It triggers at the end of the maneuver.
     */
    private class StopSwitch implements OrekitSwitchingFunction {

        /** Serializable UID. */
        private static final long serialVersionUID = -3870095515033978202L;

        /** {@inheritDoc} */
        public int eventOccurred(final SpacecraftState s) {
            // stop the maneuver
            firing = false;
            return RESET_DERIVATIVES;
        }

        /** The G-function is the difference between the end date and the current date.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         */
        public double g(final SpacecraftState s) {
            return endDate.minus(s.getDate());
        }

        /** {@inheritDoc} */
        public double getMaxCheckInterval() {
            return duration;
        }

        /** {@inheritDoc} */
        public double getThreshold() {
            // convergence threshold in seconds
            return 1.0e-4;
        }

        /** {@inheritDoc} */
        public int getMaxIterationCount() {
            return 10;
        }

        /** {@inheritDoc} */
        public SpacecraftState resetState(final SpacecraftState oldState)
            throws OrekitException {
            // never called since eventOccurred does never return RESET_STATE
            return null;
        }

    }

}
