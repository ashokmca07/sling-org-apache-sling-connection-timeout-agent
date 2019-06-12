/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.uca.impl;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.bytecode.Descriptor;

/**
 * Sets timeouts for HTTP calls done using <em>OkHttp 3.x</em>
 * 
 * <p>It inserts two calls to <tt>okhttp3.OkHttpClient$Builder</tt> that set default
 * values for <tt>connectTimeout</tt> and <tt>readTimeout</tt>.</p>
 */
public class OkHttpTimeoutTransformer implements ClassFileTransformer {

    private static final String REQUEST_CONFIG_BUILDER_CLASS_NAME = Descriptor.toJvmName("okhttp3.OkHttpClient$Builder");
    
    private final long connectTimeoutMillis;
    private final long readTimeoutMillis;
    
    public OkHttpTimeoutTransformer(long connectTimeoutMillis, long readTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            if ( REQUEST_CONFIG_BUILDER_CLASS_NAME.equals(className) ) {
                Log.get().log("%s asked to transform %s", getClass().getSimpleName(), className);
                
                ClassPool defaultPool = ClassPool.getDefault();
                CtClass cc = defaultPool.get(Descriptor.toJavaName(className));
                
                CtConstructor noArgCtor = cc.getConstructor(Descriptor.ofConstructor(new CtClass[0]));
                CtField connectTimeout = cc.getDeclaredField("connectTimeout");
                CtField readTimeout = cc.getDeclaredField("readTimeout");
                noArgCtor.insertAfter("this." + connectTimeout.getName() + " = " + connectTimeoutMillis + ";");
                noArgCtor.insertAfter("this." + readTimeout.getName() + " = " + readTimeoutMillis + ";");
                
                classfileBuffer = cc.toBytecode();
                cc.detach();
                Log.get().log("Transformation complete.");
            }
            return classfileBuffer;
        } catch (Exception e) {
            Log.get().fatal("Transformation failed", e);
            return null;
        }
    }

}