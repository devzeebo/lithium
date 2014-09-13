package com.zeebo.lithium.mortalis

/**
 * Created with IntelliJ IDEA.
 * User: Eric Siebeneich
 * Date: 9/13/14
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */
class DelegatingObject implements GroovyInterceptable {

	private Closure preInvoke = null

	private Object delegate = null

	private Closure postInvoke = null

	def invokeMethod(String name, args) {
		def ret = null
		if (this.@preInvoke != null) {
			this.@preInvoke(name, args)
		}
		if (args.size() > 0) {
			ret = this.@delegate?."${name}"(*args)
		}
		else {
			ret = this.@delegate?."${name}"()
		}
		if (this.@postInvoke != null) {
			this.@postInvoke(name, args)
		}

		return ret
	}
	def getProperty(String name) {
		return this.@delegate?."${name}"
	}
	void setProperty(String name, value) {
		this.@delegate."${name}" = value
	}

	String toString() {
		return this?.@delegate?.toString()
	}
}
