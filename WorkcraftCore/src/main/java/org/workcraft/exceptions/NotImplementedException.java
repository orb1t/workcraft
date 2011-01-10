/*
*
* Copyright 2008,2009 Newcastle University
*
* This file is part of Workcraft.
* 
* Workcraft is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Workcraft is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Workcraft.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package org.workcraft.exceptions;


public class NotImplementedException extends RuntimeException {

	// To warn users that they should implement the feature or use NotSupportedException instead
	@Deprecated
	public NotImplementedException() 
	{
		super("The feature is not implemented yet");
	}
	
	// To warn users that they should implement the feature or use NotSupportedException instead
	@Deprecated
	public NotImplementedException(String message) 
	{
		super(message);
	}
	
	private static final long serialVersionUID = -6828334836877473788L;

}