/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.modref;

import java.util.Collection;
import java.util.Map;

import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.BitVectorUnionVector;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * Generic dataflow framework to accumulate reachable gen'ned values in a graph.
 * 
 * @author sjfink
 * 
 */
public class GenReach<T, L> extends BitVectorFramework<T, L> {

  @SuppressWarnings("unchecked")
  public GenReach(Graph<T> flowGraph, Map<T, Collection<L>> gen) {
    super(flowGraph, new GenFunctions<T, L>(gen), makeDomain(gen));
    // ugly but necessary, in order to avoid computing the domain twice.
    GenReach.GenFunctions<T, L> g = (GenReach.GenFunctions<T, L>) getTransferFunctionProvider();
    g.domain = getLatticeValues();
  }

  private static <T, L> OrdinalSetMapping<L> makeDomain(Map<T, Collection<L>> gen) {
    MutableMapping<L> result = MutableMapping.make();
    for (Collection<L> c : gen.values()) {
      for (L p : c) {
        result.add(p);
      }
    }
    return result;
  }

  static class GenFunctions<T, L> implements ITransferFunctionProvider<T, BitVectorVariable> {
    private final Map<T, Collection<L>> gen;

    private OrdinalSetMapping<L> domain;

    public GenFunctions(Map<T, Collection<L>> gen) {
      this.gen = gen;
    }

    public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
      return BitVectorUnion.instance();
    }

    public UnaryOperator<BitVectorVariable> getNodeTransferFunction(T node) {
      BitVector v = getGen(node);
      return new BitVectorUnionVector(v);
    }

    private BitVector getGen(T node) {
      Collection<L> g = gen.get(node);
      if (g == null) {
        return new BitVector();
      } else {
        BitVector result = new BitVector();
        for (L p : g) {
          result.set(domain.getMappedIndex(p));
        }
        return result;
      }
    }

    public boolean hasEdgeTransferFunctions() {
      return false;
    }

    public boolean hasNodeTransferFunctions() {
      return true;
    }

    public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(T src, T dst) {
      Assertions.UNREACHABLE();
      return null;
    }

  }
}