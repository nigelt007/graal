/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.hotspot.replacements.arraycopy;

import static jdk.vm.ci.meta.LocationIdentity.any;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.LocationIdentity;

import com.oracle.graal.compiler.common.type.StampFactory;
import com.oracle.graal.graph.NodeClass;
import com.oracle.graal.nodeinfo.InputType;
import com.oracle.graal.nodeinfo.NodeInfo;
import com.oracle.graal.nodes.NamedLocationIdentity;
import com.oracle.graal.nodes.ValueNode;
import com.oracle.graal.nodes.extended.ArrayRangeWriteNode;
import com.oracle.graal.nodes.memory.MemoryAccess;
import com.oracle.graal.nodes.memory.MemoryCheckpoint;
import com.oracle.graal.nodes.memory.MemoryNode;
import com.oracle.graal.nodes.spi.Lowerable;
import com.oracle.graal.nodes.spi.LoweringTool;

@NodeInfo(allowedUsageTypes = InputType.Memory)
public class ArrayCopyUnrollNode extends ArrayRangeWriteNode implements MemoryCheckpoint.Single, Lowerable, MemoryAccess {

    public static final NodeClass<ArrayCopyUnrollNode> TYPE = NodeClass.create(ArrayCopyUnrollNode.class);

    @Input protected ValueNode src;
    @Input protected ValueNode srcPos;
    @Input protected ValueNode dest;
    @Input protected ValueNode destPos;
    @Input protected ValueNode length;

    private JavaKind elementKind;

    private int unrolledLength;

    @OptionalInput(InputType.Memory) private MemoryNode lastLocationAccess;

    public ArrayCopyUnrollNode(ValueNode src, ValueNode srcPos, ValueNode dest, ValueNode destPos, ValueNode length, int unrolledLength, JavaKind elementKind) {
        super(TYPE, StampFactory.forKind(JavaKind.Void));
        this.src = src;
        this.srcPos = srcPos;
        this.dest = dest;
        this.destPos = destPos;
        this.length = length;
        this.unrolledLength = unrolledLength;
        assert elementKind != null && elementKind != JavaKind.Illegal;
        this.elementKind = elementKind;
    }

    public ValueNode getSource() {
        return src;
    }

    public ValueNode getSourcePosition() {
        return srcPos;
    }

    public ValueNode getDestination() {
        return dest;
    }

    public ValueNode getDestinationPosition() {
        return destPos;
    }

    @Override
    public ValueNode getLength() {
        return length;
    }

    @Override
    public ValueNode getArray() {
        return dest;
    }

    @Override
    public ValueNode getIndex() {
        return destPos;
    }

    @Override
    public boolean isObjectArray() {
        return elementKind == JavaKind.Object;
    }

    @Override
    public boolean isInitialization() {
        return false;
    }

    @NodeIntrinsic
    public static native void arraycopy(Object nonNullSrc, int srcPos, Object nonNullDest, int destPos, int length, @ConstantNodeParameter int unrolledLength,
                    @ConstantNodeParameter JavaKind elementKind);

    public int getUnrollLength() {
        return unrolledLength;
    }

    public JavaKind getElementKind() {
        return elementKind;
    }

    @Override
    public LocationIdentity getLocationIdentity() {
        if (elementKind != null) {
            return NamedLocationIdentity.getArrayLocation(elementKind);
        }
        return any();
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public MemoryNode getLastLocationAccess() {
        return lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode lla) {
        updateUsagesInterface(lastLocationAccess, lla);
        lastLocationAccess = lla;
    }
}