/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.frames;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.utils.PVCoordinates;


/** Transformation class in three dimensional space.
 *
 * <p>This class represents the transformation engine between {@link Frame frames}.
 * It is used both to define the relationship between each frame and its
 * parent frame and to gather all individual transforms into one
 * operation when converting between frames far away from each other.</p>
 * <p> The convention used in OREKIT is vectorial transformation. It means
 * that a transformation is defined as a transform to apply to the
 * coordinates of a vector expressed in the old frame to obtain the
 * same vector expressed in the new frame. <p>
 *
 * <p>Instances of this class are guaranteed to be immutable.</p>
 *
 *  <h5> Example </h5>
 *
 * <pre>
 *
 * 1 ) Example of translation from R<sub>A</sub> to R<sub>B</sub>:
 * We want to transform the {@link PVCoordinates} PV<sub>A</sub> to PV<sub>B</sub>.
 *
 * With :  PV<sub>A</sub> = ({1, 0, 0} , {1, 0, 0});
 * and  :  PV<sub>B</sub> = ({0, 0, 0} , {0, 0, 0});
 *
 * The transform to apply then is defined as follows :
 *
 * Vector3D translation = new Vector3D(-1,0,0);
 * Vector3D velocity = new Vector3D(-1,0,0);
 *
 * Transform R1toR2 = new Transform(translation, Velocity);
 *
 * PV<sub>B</sub> = R1toR2.transformPVCoordinates(PV<sub>A</sub>);
 *
 *
 * 2 ) Example of rotation from R<sub>A</sub> to R<sub>B</sub>:
 * We want to transform the {@link PVCoordinates} PV<sub>A</sub> to PV<sub>B</sub>.
 *
 * With :  PV<sub>A</sub> = ({1, 0, 0}, {1, 0, 0});
 * and  :  PV<sub>B</sub> = ({0, 1, 0}, {-2, 1, 0});
 *
 * The transform to apply then is defined as follows :
 *
 * Rotation rotation = new Rotation(Vector3D.PLUS_K, FastMath.PI / 2);
 * Vector3D rotationRate = new Vector3D(0, 0, -2);
 *
 * Transform R1toR2 = new Transform(rotation, rotationRate);
 *
 * PV<sub>B</sub> = R1toR2.transformPVCoordinates(PV<sub>A</sub>);
 *
 * </pre>
 *
 * @author Luc Maisonobe
 * @author Fabien Maussion
 */
public class Transform implements Serializable {

    /** Identity transform. */
    public static final Transform IDENTITY = new IdentityTransform();

    /** Serializable UID. */
    private static final long serialVersionUID = -9008113096602590296L;

    /** Global translation. */
    private final Vector3D translation;

    /** First time derivative of the translation. */
    private final Vector3D velocity;

    /** Global rotation. */
    private final Rotation rotation;

    /** First time derivative of the rotation (norm representing angular rate). */
    private final Vector3D rotationRate;

    /** Build a transform from its primitive operations.
     * @param translation first primitive operation to apply
     * @param velocity first time derivative of the translation
     * @param rotation second primitive operation to apply
     * @param rotationRate first time derivative of the rotation (norm representing angular rate)
     */
    private Transform(final Vector3D translation, final Vector3D velocity,
                      final Rotation rotation, final Vector3D rotationRate) {
        this.translation  = translation;
        this.rotation     = rotation;
        this.velocity     = velocity;
        this.rotationRate = rotationRate;
    }

    /** Build a translation transform.
     * @param translation translation to apply (i.e. coordinates of
     * the transformed origin, or coordinates of the origin of the
     * old frame in the new frame)
     */
    public Transform(final Vector3D translation) {
        this(translation, Vector3D.ZERO, Rotation.IDENTITY, Vector3D.ZERO);
    }

    /** Build a rotation transform.
     * @param rotation rotation to apply ( i.e. rotation to apply to the
     * coordinates of a vector expressed in the old frame to obtain the
     * same vector expressed in the new frame )
     */
    public Transform(final Rotation rotation) {
        this(Vector3D.ZERO, Vector3D.ZERO, rotation, Vector3D.ZERO);
    }

    /** Build a translation transform, with its first time derivative.
     * @param translation translation to apply (i.e. coordinates of
     * the transformed origin, or coordinates of the origin of the
     * old frame in the new frame)
     * @param velocity the velocity of the translation (i.e. origin
     * of the old frame velocity in the new frame)
     */
    public Transform(final Vector3D translation, final Vector3D velocity) {
        this(translation, velocity, Rotation.IDENTITY, Vector3D.ZERO);
    }

    /** Build a rotation transform.
     * @param rotation rotation to apply ( i.e. rotation to apply to the
     * coordinates of a vector expressed in the old frame to obtain the
     * same vector expressed in the new frame )
     * @param rotationRate the axis of the instant rotation
     * expressed in the new frame. (norm representing angular rate)
     */
    public Transform(final Rotation rotation, final Vector3D rotationRate) {
        this(Vector3D.ZERO, Vector3D.ZERO, rotation, rotationRate);
    }

    /** Build a transform by combining two existing ones.
     * @param first first transform applied
     * @param second second transform applied
     */
    public Transform(final Transform first, final Transform second) {
        this(compositeTranslation(first, second), compositeVelocity(first, second),
             compositeRotation(first, second), compositeRotationRate(first, second));
    }

    /** Compute a composite translation.
     * @param first first applied transform
     * @param second second applied transform
     * @return translation part of the composite transform
     */
    private static Vector3D compositeTranslation(final Transform first, final Transform second) {
        return first.translation.add(first.rotation.applyInverseTo(second.translation));
    }

    /** Compute a composite velocity.
     * @param first first applied transform
     * @param second second applied transform
     * @return velocity part of the composite transform
     */
    private static Vector3D compositeVelocity(final Transform first, final Transform second) {
        final Vector3D cross =
            Vector3D.crossProduct(first.rotationRate, second.translation);
        return first.velocity.add(first.rotation.applyInverseTo(second.velocity.add(cross)));
    }

    /** Compute a composite rotation.
     * @param first first applied transform
     * @param second second applied transform
     * @return rotation part of the composite transform
     */
    private static Rotation compositeRotation(final Transform first, final Transform second) {
        return second.rotation.applyTo(first.rotation);
    }

    /** Compute a composite rotation rate.
     * @param first first applied transform
     * @param second second applied transform
     * @return rotation rate part of the composite transform
     */
    private static Vector3D compositeRotationRate(final Transform first, final Transform second) {
        return second.rotationRate.add(second.rotation.applyTo(first.rotationRate));
    }

    /** Get the inverse transform of the instance.
     * @return inverse transform of the instance
     */
    public Transform getInverse() {
        final Vector3D rT = rotation.applyTo(translation);
        return new Transform(rT.negate(),
                             Vector3D.crossProduct(rotationRate, rT).subtract(rotation.applyTo(velocity)),
                             rotation.revert(),
                             rotation.applyInverseTo(rotationRate.negate()));
    }

    /** Get a freezed transform.
     * <p>
     * This method creates a copy of the instance but frozen in time,
     * i.e. with velocity and rotation rate forced to zero.
     * </p>
     * @return a new transform, without any time-dependent parts
     */
    public Transform freeze() {
        return new Transform(translation, Vector3D.ZERO, rotation, Vector3D.ZERO);
    }

    /** Transform a position vector (including translation effects).
     * @param position vector to transform
     * @return transformed position
     */
    public Vector3D transformPosition(final Vector3D position) {
        return rotation.applyTo(translation.add(position));
    }

    /** Transform a vector (ignoring translation effects).
     * @param vector vector to transform
     * @return transformed vector
     */
    public Vector3D transformVector(final Vector3D vector) {
        return rotation.applyTo(vector);
    }

    /** Transform a line.
     * @param line to transform
     * @return transformed line
     */
    public Line transformLine(final Line line) {
        final Vector3D transformedP0 = transformPosition(line.getOrigin());
        final Vector3D transformedP1 = transformPosition(line.pointAt(1.0e6));
        return new Line(transformedP0, transformedP1);
    }

    /** Transform {@link PVCoordinates} including kinematic effects.
     * @param pv the couple position-velocity to transform.
     * @return transformed position/velocity
     */
    public PVCoordinates transformPVCoordinates(final PVCoordinates pv) {
        final Vector3D p = pv.getPosition();
        final Vector3D v = pv.getVelocity();
        final Vector3D transformedP = rotation.applyTo(translation.add(p));
        final Vector3D cross = Vector3D.crossProduct(rotationRate, transformedP);
        return new PVCoordinates(transformedP,
                                 rotation.applyTo(v.add(velocity)).subtract(cross));
    }

    /** Compute the Jacobian of the {@link #transformPVCoordinates(PVCoordinates)}
     * method of the transform.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of Cartesian coordinate i
     * of the transformed {@link PVCoordinates} with respect to Cartesian coordinate j
     * of the input {@link PVCoordinates} in method {@link #transformPVCoordinates(PVCoordinates)}.
     * </p>
     * <p>
     * This definition implies that if we define position-velocity coordinates
     * <pre>
     * PV<sub>1</sub> = transform.transformPVCoordinates(PV<sub>0</sub>), then
     * </pre>
     * their differentials dPV<sub>1</sub> and dPV<sub>0</sub> will obey the following relation
     * where J is the matrix computed by this method:<br/>
     * <pre>
     * dPV<sub>1</sub> = J &times; dPV<sub>0</sub>
     * </pre>
     * </p>
     * @param jacobian placeholder 6x6 (or larger) matrix to be filled with the Jacobian, if matrix
     * is larger than 6x6, only the 6x6 upper left corner will be modified
     */
    public void getJacobian(final double[][] jacobian) {

        // elementary matrix for rotation
        final double[][] mData = rotation.getMatrix();

        // dP1/dP0
        System.arraycopy(mData[0], 0, jacobian[0], 0, 3);
        System.arraycopy(mData[1], 0, jacobian[1], 0, 3);
        System.arraycopy(mData[2], 0, jacobian[2], 0, 3);

        // dP1/dV0
        Arrays.fill(jacobian[0], 3, 6, 0.0);
        Arrays.fill(jacobian[1], 3, 6, 0.0);
        Arrays.fill(jacobian[2], 3, 6, 0.0);

        // dV1/dP0
        final double mOx = -rotationRate.getX();
        final double mOy = -rotationRate.getY();
        final double mOz = -rotationRate.getZ();
        for (int i = 0; i < 3; ++i) {
            jacobian[3][i] = mOy * mData[2][i] - mOz * mData[1][i];
            jacobian[4][i] = mOz * mData[0][i] - mOx * mData[2][i];
            jacobian[5][i] = mOx * mData[1][i] - mOy * mData[0][i];
        }

        // dV1/dV0
        System.arraycopy(mData[0], 0, jacobian[3], 3, 3);
        System.arraycopy(mData[1], 0, jacobian[4], 3, 3);
        System.arraycopy(mData[2], 0, jacobian[5], 3, 3);

    }

    /** Get the underlying elementary translation.
     * <p>A transform can be uniquely represented as an elementary
     * translation followed by an elementary rotation. This method
     * returns this unique elementary translation.</p>
     * @return underlying elementary translation
     * @see #getRotation()
     */
    public Vector3D getTranslation() {
        return translation;
    }

    /** Get the first time derivative of the translation.
     * @return first time derivative of the translation
     */
    public Vector3D getVelocity() {
        return velocity;
    }

    /** Get the underlying elementary rotation.
     * <p>A transform can be uniquely represented as an elementary
     * translation followed by an elementary rotation. This method
     * returns this unique elementary rotation.</p>
     * @return underlying elementary rotation
     * @see #getTranslation()
     */
    public Rotation getRotation() {
        return rotation;
    }

    /** Get the first time derivative of the rotation.
     * <p>The norm represents the angular rate.</p>
     * @return First time derivative of the rotation
     */
    public Vector3D getRotationRate() {
        return rotationRate;
    }

    /** Specialized class for identity transform. */
    private static class IdentityTransform extends Transform {

        /** Serializable UID. */
        private static final long serialVersionUID = -9042082036141830517L;

        /** Simple constructor. */
        public IdentityTransform() {
            super(Vector3D.ZERO, Vector3D.ZERO, Rotation.IDENTITY, Vector3D.ZERO);
        }

        /** {@inheritDoc} */
        @Override
        public Transform getInverse() {
            return this;
        };

        /** {@inheritDoc} */
        @Override
        public Vector3D transformPosition(final Vector3D position) {
            return position;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D transformVector(final Vector3D vector) {
            return vector;
        }

        /** {@inheritDoc} */
        @Override
        public Line transformLine(final Line line) {
            return line;
        }

        /** {@inheritDoc} */
        @Override
        public PVCoordinates transformPVCoordinates(final PVCoordinates pv) {
            return pv;
        }

        /** {@inheritDoc} */
        @Override
        public void getJacobian(final double[][] jacobian) {
            for (int i = 0; i < 6; ++i) {
                Arrays.fill(jacobian[i], 0, 6, 0.0);
                jacobian[i][i] = 1.0;
            }
        }

    };

}
