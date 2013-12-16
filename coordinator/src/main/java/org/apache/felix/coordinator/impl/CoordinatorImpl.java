/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.coordinator.impl;

import java.util.Collection;
import java.util.TimerTask;

import org.osgi.framework.Bundle;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;

/**
 * The coordinator implementation is a per bundle wrapper for the
 * coordination manager.
 */
public class CoordinatorImpl implements Coordinator
{

    /** The bundle that requested this service. */
    private final Bundle owner;

    /** The coordination mgr. */
    private final CoordinationMgr mgr;

    CoordinatorImpl(final Bundle owner, final CoordinationMgr mgr)
    {
        this.owner = owner;
        this.mgr = mgr;
    }

    /**
     * Ensure all active Coordinations started by this CoordinatorImpl instance
     * are terminated before the service is ungotten by the bundle.
     * <p>
     * Called by the Coordinator ServiceFactory when this CoordinatorImpl
     * instance is not used any longer by the owner bundle.
     *
     * @see FELIX-2671/OSGi Bug 104
     */
    void dispose()
    {
        this.mgr.dispose(this.owner);
    }

    /**
     * Ensures the <code>name</code> complies with the <em>symbolic-name</em>
     * production of the OSGi core specification (1.3.2):
     *
     * <pre>
     * symbolic-name :: = token('.'token)*
     * digit    ::= [0..9]
     * alpha    ::= [a..zA..Z]
     * alphanum ::= alpha | digit
     * token    ::= ( alphanum | ’_’ | ’-’ )+
     * </pre>
     *
     * If the key does not comply an <code>IllegalArgumentException</code> is
     * thrown.
     *
     * @param key
     *            The configuration property key to check.
     * @throws IllegalArgumentException
     *             if the key does not comply with the symbolic-name production.
     */
    private void checkName( final String name )
    {
        // check for empty string
        if ( name.length() == 0 )
        {
            throw new IllegalArgumentException( "Name must not be an empty string" );
        }
        final String[] parts = name.split("\\.");
        for(final String p : parts)
        {
        	boolean valid = true;
        	if ( p.length() == 0 )
        	{
        		valid = false;
        	}
        	else
        	{
	            for(int i=0; i<p.length(); i++)
	            {
	            	final char c = p.charAt(i);
	            	if ( c >= '0' && c <= '9') {
	            		continue;
	            	}
	            	if ( c >= 'a' && c <= 'z') {
	            		continue;
	            	}
	            	if ( c >= 'A' && c <= 'Z') {
	            		continue;
	            	}
	            	if ( c == '_' || c == '-') {
	            		continue;
	            	}
	            	valid = false;
	            	break;
	            }
        	}
        	if ( !valid )
        	{
                throw new IllegalArgumentException( "Name [" + name + "] does not comply with the symbolic-name definition." );
        	}
        }
    }

    /**
     * @see org.osgi.service.coordinator.Coordinator#create(java.lang.String, long)
     */
    public Coordination create(final String name, final long timeout)
    {
        // TODO: check permission

    	// check arguments
    	checkName(name);
    	if ( timeout < 0 )
    	{
    		throw new IllegalArgumentException("Timeout must not be negative");
    	}

    	// create coordination
        final Coordination c = mgr.create(this, name, timeout);

        return c;
    }

    /**
     * @see org.osgi.service.coordinator.Coordinator#getCoordinations()
     */
    public Collection<Coordination> getCoordinations()
    {
        // TODO: check permission
        return mgr.getCoordinations();
    }

    /**
     * @see org.osgi.service.coordinator.Coordinator#fail(java.lang.Throwable)
     */
    public boolean fail(final Throwable reason)
    {
        // TODO: check permission
        CoordinationImpl current = (CoordinationImpl) peek();
        if (current != null)
        {
            return current.fail(reason);
        }
        return false;
    }

    /**
     * @see org.osgi.service.coordinator.Coordinator#peek()
     */
    public Coordination peek()
    {
        // TODO: check permission
        return mgr.peek();
    }

    /**
     * @see org.osgi.service.coordinator.Coordinator#begin(java.lang.String, long)
     */
    public Coordination begin(final String name, final long timeoutInMillis)
    {
        // TODO: check permission
        final Coordination c = create(name, timeoutInMillis);
        this.mgr.push((CoordinationImpl)c);
        return c;
    }

    /**
     * @see org.osgi.service.coordinator.Coordinator#pop()
     */
    public Coordination pop()
    {
        // TODO: check permission
        return mgr.pop();
    }

    /**
     * @see org.osgi.service.coordinator.Coordinator#addParticipant(org.osgi.service.coordinator.Participant)
     */
    public boolean addParticipant(Participant participant)
    {
        // TODO: check permission
        Coordination current = peek();
        if (current != null)
        {
            current.addParticipant(participant);
            return true;
        }
        return false;
    }

    /**
     * @see org.osgi.service.coordinator.Coordinator#getCoordination(long)
     */
    public Coordination getCoordination(final long id)
    {
        // TODO: check permission
        return mgr.getCoordinationById(id);
    }

    //----------

    void push(final CoordinationImpl c)
    {
        mgr.push(c);
    }

    void unregister(final CoordinationImpl c, final boolean removeFromStack)
    {
        mgr.unregister(c, removeFromStack);
    }

    void schedule(final TimerTask task, final long deadLine)
    {
        mgr.schedule(task, deadLine);
    }

    void lockParticipant(final Participant p, final CoordinationImpl c)
    {
        mgr.lockParticipant(p, c);
    }

    void releaseParticipant(final Participant p)
    {
        mgr.releaseParticipant(p);
    }

    Bundle getBundle()
    {
        return this.owner;
    }

	Coordination getEnclosingCoordination(final CoordinationImpl c)
	{
		return mgr.getEnclosingCoordination(c);
	}

	boolean endNestedCoordinations(final CoordinationImpl c)
	{
		return this.mgr.endNestedCoordinations(c);
	}
}
