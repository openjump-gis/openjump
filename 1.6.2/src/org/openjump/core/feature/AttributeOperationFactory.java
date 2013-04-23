/*
Copyright (c) 2012, Michaël Michaud
All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of its authors nor the names of its contributors may
      be used to endorse or promote products derived from this software without
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS "AS IS" AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.openjump.core.feature;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Operation;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.model.Layer;

import java.util.HashMap;
import java.util.Map;

/**
 * An AttributeOperationFactory can create an Operation returning values of
 * a certain AttributeType.
 *
 * @author Micha&euml;l Michaud
 * @version 0.1 (2012-11-17)
 */
 // 0.1 (2012-11-17)
public abstract class AttributeOperationFactory {
    
    // /////////////////////////////////////////////////////////////////////////
    // Static part (Registry containing OperationFactories)
    // /////////////////////////////////////////////////////////////////////////
    
    private static Map<String,AttributeOperationFactory> registry = 
                   new HashMap<String,AttributeOperationFactory>();
    
    /** Add an OperationFactory in the Registry*/
    private static void addOperationFactory(AttributeOperationFactory factory) {
        registry.put(factory.getOperationClass().getName(), factory);
    }
    
    /** Get an OperationFactory from the Registry*/
    public static AttributeOperationFactory getFactory(String operationClass) {
        return registry.get(operationClass);
    }
    
    // /////////////////////////////////////////////////////////////////////////
    // Instance part representing a particular OperationFactory
    // /////////////////////////////////////////////////////////////////////////
    
    public PlugInContext context;
    
    public AttributeOperationFactory(PlugInContext context) {
        this.context = context;
        addOperationFactory(this);
    }
    
    abstract public Operation createOperation(AttributeType type, String expression) throws Error;
    
    abstract public Class getOperationClass();
    
}
