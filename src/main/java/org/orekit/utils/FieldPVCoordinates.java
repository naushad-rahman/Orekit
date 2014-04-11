/* Copyright 2002-2014 CS Systèmes d'Information
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
package org.orekit.utils;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.math3.RealFieldElement;
import org.apache.commons.math3.analysis.interpolation.FieldHermiteInterpolator;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;

/** Simple container for Position/Velocity pairs, using {@link RealFieldElement}.
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * a simple linear model. It is <em>not</em> intended as a replacement for
 * proper orbit propagation (it is not even Keplerian!) but should be sufficient
 * for either small time shifts or coarse accuracy.
 * </p>
 * <p>
 * This class is the angular counterpart to {@link FieldAngularCoordinates}.
 * </p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 * @since 6.0
 * @see PVCoordinates
 */
public class FieldPVCoordinates<T extends RealFieldElement<T>>
    implements TimeShiftable<FieldPVCoordinates<T>>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140411L;

    /** The position. */
    private final FieldVector3D<T> position;

    /** The velocity. */
    private final FieldVector3D<T> velocity;

    /** The acceleration. */
    private final FieldVector3D<T> acceleration;

    /** Builds a PVCoordinates triplet with zero acceleration.
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     */
    public FieldPVCoordinates(final FieldVector3D<T> position, final FieldVector3D<T> velocity) {
        this.position     = position;
        this.velocity     = velocity;
        final T zero      = position.getX().getField().getZero();
        this.acceleration = new FieldVector3D<T>(zero, zero, zero);
    }

    /** Builds a PVCoordinates triplet.
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     * @param acceleration the acceleration vector (m/s²)
     */
    public FieldPVCoordinates(final FieldVector3D<T> position, final FieldVector3D<T> velocity,
                              final FieldVector3D<T> acceleration) {
        this.position     = position;
        this.velocity     = velocity;
        this.acceleration = acceleration;
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final double a, final FieldPVCoordinates<T> pv) {
        position     = new FieldVector3D<T>(a, pv.position);
        velocity     = new FieldVector3D<T>(a, pv.velocity);
        acceleration = new FieldVector3D<T>(a, pv.acceleration);
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a, final FieldPVCoordinates<T> pv) {
        position     = new FieldVector3D<T>(a, pv.position);
        velocity     = new FieldVector3D<T>(a, pv.velocity);
        acceleration = new FieldVector3D<T>(a, pv.acceleration);
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a, final PVCoordinates pv) {
        position     = new FieldVector3D<T>(a, pv.getPosition());
        velocity     = new FieldVector3D<T>(a, pv.getVelocity());
        acceleration = new FieldVector3D<T>(a, pv.getAcceleration());
    }

    /** Subtractive constructor
     * <p>Build a relative PVCoordinates from a start and an end position.</p>
     * <p>The PVCoordinates built will be end - start.</p>
     * @param start Starting PVCoordinates
     * @param end ending PVCoordinates
     */
    public FieldPVCoordinates(final FieldPVCoordinates<T> start, final FieldPVCoordinates<T> end) {
        this.position     = end.position.subtract(start.position);
        this.velocity     = end.velocity.subtract(start.velocity);
        this.acceleration = end.acceleration.subtract(start.acceleration);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final double a1, final FieldPVCoordinates<T> pv1,
                              final double a2, final FieldPVCoordinates<T> pv2) {
        position     = new FieldVector3D<T>(a1, pv1.position, a2, pv2.position);
        velocity     = new FieldVector3D<T>(a1, pv1.velocity, a2, pv2.velocity);
        acceleration = new FieldVector3D<T>(a1, pv1.acceleration, a2, pv2.acceleration);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final FieldPVCoordinates<T> pv1,
                              final T a2, final FieldPVCoordinates<T> pv2) {
        position     = new FieldVector3D<T>(a1, pv1.position, a2, pv2.position);
        velocity     = new FieldVector3D<T>(a1, pv1.velocity, a2, pv2.velocity);
        acceleration = new FieldVector3D<T>(a1, pv1.acceleration, a2, pv2.acceleration);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final PVCoordinates pv1,
                              final T a2, final PVCoordinates pv2) {
        position     = new FieldVector3D<T>(a1, pv1.getPosition(), a2, pv2.getPosition());
        velocity     = new FieldVector3D<T>(a1, pv1.getVelocity(), a2, pv2.getVelocity());
        acceleration = new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration());
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final double a1, final FieldPVCoordinates<T> pv1,
                           final double a2, final FieldPVCoordinates<T> pv2,
                           final double a3, final FieldPVCoordinates<T> pv3) {
        position     = new FieldVector3D<T>(a1, pv1.position,     a2, pv2.position,     a3, pv3.position);
        velocity     = new FieldVector3D<T>(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity);
        acceleration = new FieldVector3D<T>(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final FieldPVCoordinates<T> pv1,
                              final T a2, final FieldPVCoordinates<T> pv2,
                              final T a3, final FieldPVCoordinates<T> pv3) {
        position     = new FieldVector3D<T>(a1, pv1.position,     a2, pv2.position,     a3, pv3.position);
        velocity     = new FieldVector3D<T>(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity);
        acceleration = new FieldVector3D<T>(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final PVCoordinates pv1,
                              final T a2, final PVCoordinates pv2,
                              final T a3, final PVCoordinates pv3) {
        position     = new FieldVector3D<T>(a1, pv1.getPosition(),     a2, pv2.getPosition(),     a3, pv3.getPosition());
        velocity     = new FieldVector3D<T>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),     a3, pv3.getVelocity());
        acceleration = new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(), a3, pv3.getAcceleration());
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     * @param a4 fourth scale factor
     * @param pv4 fourth base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final double a1, final FieldPVCoordinates<T> pv1,
                              final double a2, final FieldPVCoordinates<T> pv2,
                              final double a3, final FieldPVCoordinates<T> pv3,
                              final double a4, final FieldPVCoordinates<T> pv4) {
        position     = new FieldVector3D<T>(a1, pv1.position,     a2, pv2.position,     a3, pv3.position,     a4, pv4.position);
        velocity     = new FieldVector3D<T>(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity,     a4, pv4.velocity);
        acceleration = new FieldVector3D<T>(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration, a4, pv4.acceleration);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     * @param a4 fourth scale factor
     * @param pv4 fourth base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final FieldPVCoordinates<T> pv1,
                              final T a2, final FieldPVCoordinates<T> pv2,
                              final T a3, final FieldPVCoordinates<T> pv3,
                              final T a4, final FieldPVCoordinates<T> pv4) {
        position     = new FieldVector3D<T>(a1, pv1.position,     a2, pv2.position,     a3, pv3.position,     a4, pv4.position);
        velocity     = new FieldVector3D<T>(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity,     a4, pv4.velocity);
        acceleration = new FieldVector3D<T>(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration, a4, pv4.acceleration);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     * @param a4 fourth scale factor
     * @param pv4 fourth base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final PVCoordinates pv1,
                              final T a2, final PVCoordinates pv2,
                              final T a3, final PVCoordinates pv3,
                              final T a4, final PVCoordinates pv4) {
        position     = new FieldVector3D<T>(a1, pv1.getPosition(),     a2, pv2.getPosition(),
                                            a3, pv3.getPosition(),     a4, pv4.getPosition());
        velocity     = new FieldVector3D<T>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),
                                            a3, pv3.getVelocity(),     a4, pv4.getVelocity());
        acceleration = new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(),
                                            a3, pv3.getAcceleration(), a4, pv4.getAcceleration());
    }

    /** Estimate velocity between two positions.
     * <p>Estimation is based on a simple fixed velocity translation
     * during the time interval between the two positions.</p>
     * @param start start position
     * @param end end position
     * @param dt time elapsed between the dates of the two positions
     * @param <T> the type of the field elements
     * @return velocity allowing to go from start to end positions
     */
    public static <T extends RealFieldElement<T>> FieldVector3D<T> estimateVelocity(final FieldVector3D<T> start,
                                                                                    final FieldVector3D<T> end,
                                                                                    final double dt) {
        final double scale = 1.0 / dt;
        return new FieldVector3D<T>(scale, end, -scale, start);
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple quadratic model. It is <em>not</em> intended as a replacement for
     * proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public FieldPVCoordinates<T> shiftedBy(final double dt) {
        return new FieldPVCoordinates<T>(new FieldVector3D<T>(1, position, dt, velocity, 0.5 * dt * dt, acceleration),
                                         new FieldVector3D<T>(1, velocity, dt, acceleration),
                                         acceleration);
    }

    /** Interpolate position-velocity.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * ensuring velocity remains the exact derivative of position.
     * </p>
     * <p>
     * Note that even if first time derivatives (velocities)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the positions.
     * </p>
     * @param date interpolation date
     * @param useVelocities if true, use sample points velocities,
     * otherwise ignore them and use only positions
     * @param sample sample points on which interpolation should be done
     * @param <T> the type of the field elements
     * @return a new position-velocity, interpolated at specified date
     * @deprecated as of 7.1, replaced with {@link #interpolate(AbsoluteDate, PVASampleFilter, Collection)}
     */
    @Deprecated
    public static <T extends RealFieldElement<T>> FieldPVCoordinates<T> interpolate(final AbsoluteDate date,
                                                                                    final boolean useVelocities,
                                                                                    final Collection<Pair<AbsoluteDate, FieldPVCoordinates<T>>> sample) {
        return interpolate(date,
                           useVelocities ? PVASampleFilter.SAMPLE_PV : PVASampleFilter.SAMPLE_P,
                           sample);
    }

    /** Interpolate position-velocity.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * ensuring velocity remains the exact derivative of position.
     * </p>
     * <p>
     * Note that even if first time derivatives (velocities)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the positions.
     * </p>
     * @param date interpolation date
     * @param filter filter for derivatives to extract from sample
     * @param sample sample points on which interpolation should be done
     * @param <T> the type of the field elements
     * @return a new position-velocity, interpolated at specified date
     */
    @SuppressWarnings("unchecked")
    public static <T extends RealFieldElement<T>> FieldPVCoordinates<T> interpolate(final AbsoluteDate date,
                                                                                    final PVASampleFilter filter,
                                                                                    final Collection<Pair<AbsoluteDate, FieldPVCoordinates<T>>> sample) {

        // get field properties
        final T prototype = sample.iterator().next().getValue().getPosition().getX();
        final T zero      = prototype.getField().getZero();

        // set up an interpolator
        final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<T>();

        // add sample points
        switch (filter) {
        case SAMPLE_PVA :
            // populate sample with position, velocity and acceleration data
            for (final Pair<AbsoluteDate, FieldPVCoordinates<T>> datedPV : sample) {
                interpolator.addSamplePoint(zero.add(datedPV.getKey().getDate().durationFrom(date)),
                                            datedPV.getValue().getPosition().toArray(),
                                            datedPV.getValue().getVelocity().toArray(),
                                            datedPV.getValue().getAcceleration().toArray());
            }
            break;
        case SAMPLE_PV :
            // populate sample with position and velocity data
            for (final Pair<AbsoluteDate, FieldPVCoordinates<T>> datedPV : sample) {
                interpolator.addSamplePoint(zero.add(datedPV.getKey().getDate().durationFrom(date)),
                                            datedPV.getValue().getPosition().toArray(),
                                            datedPV.getValue().getVelocity().toArray());
            }
            break;
        case SAMPLE_P :
            // populate sample with position data, ignoring velocity
            for (final Pair<AbsoluteDate, FieldPVCoordinates<T>> datedPV : sample) {
                interpolator.addSamplePoint(zero.add(datedPV.getKey().getDate().durationFrom(date)),
                                            datedPV.getValue().getPosition().toArray());
            }
            break;
        default :
            // this should never happen
            throw OrekitException.createInternalError(null);
        }

        // interpolate
        final T[][] p = interpolator.derivatives(zero, 2);

        // build a new interpolated instance
        return new FieldPVCoordinates<T>(new FieldVector3D<T>(p[0]),
                                         new FieldVector3D<T>(p[1]),
                                         new FieldVector3D<T>(p[2]));

    }

    /** Gets the position.
     * @return the position vector (m).
     */
    public FieldVector3D<T> getPosition() {
        return position;
    }

    /** Gets the velocity.
     * @return the velocity vector (m/s).
     */
    public FieldVector3D<T> getVelocity() {
        return velocity;
    }

    /** Gets the acceleration.
     * @return the acceleration vector (m/s²).
     */
    public FieldVector3D<T> getAcceleration() {
        return acceleration;
    }

    /** Gets the momentum.
     * <p>This vector is the p &otimes; v where p is position, v is velocity
     * and &otimes; is cross product. To get the real physical angular momentum
     * you need to multiply this vector by the mass.</p>
     * <p>The returned vector is recomputed each time this method is called, it
     * is not cached.</p>
     * @return a new instance of the momentum vector (m<sup>2</sup>/s).
     */
    public FieldVector3D<T> getMomentum() {
        return FieldVector3D.crossProduct(position, velocity);
    }

    /**
     * Get the angular velocity (spin) of this point as seen from the origin.
     * <p/>
     * The angular velocity vector is parallel to the {@link #getMomentum() angular
     * momentum} and is computed by &omega; = p &times; v / ||p||<sup>2</sup>
     *
     * @return the angular velocity vector
     * @see <a href="http://en.wikipedia.org/wiki/Angular_velocity">Angular Velocity on
     *      Wikipedia</a>
     */
    public FieldVector3D<T> getAngularVelocity() {
        return this.getMomentum().scalarMultiply(
                this.getPosition().getNormSq().reciprocal());
    }

    /** Get the opposite of the instance.
     * @return a new position-velocity which is opposite to the instance
     */
    public FieldPVCoordinates<T> negate() {
        return new FieldPVCoordinates<T>(position.negate(), velocity.negate(), acceleration.negate());
    }

    /** Convert to a constant position-velocity without derivatives.
     * @return a constant position-velocity
     */
    public PVCoordinates toPVCoordinates() {
        return new PVCoordinates(position.toVector3D(), velocity.toVector3D(), acceleration.toVector3D());
    }

    /** Return a string representation of this position/velocity pair.
     * @return string representation of this position/velocity pair
     */
    public String toString() {
        final String comma = ", ";
        return new StringBuffer().append('{').append("P(").
                                  append(position.getX().getReal()).append(comma).
                                  append(position.getY().getReal()).append(comma).
                                  append(position.getZ().getReal()).append("), V(").
                                  append(velocity.getX().getReal()).append(comma).
                                  append(velocity.getY().getReal()).append(comma).
                                  append(velocity.getZ().getReal()).append("), A(").
                                  append(acceleration.getX().getReal()).append(comma).
                                  append(acceleration.getY().getReal()).append(comma).
                                  append(acceleration.getZ().getReal()).append(")}").toString();
    }

}
