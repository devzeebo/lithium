package com.zeebo.lithium.mortalis
/**
 * Created with IntelliJ IDEA.
 * User: Eric Siebeneich
 * Date: 9/12/14
 * Time: 2:14 AM
 * To change this template use File | Settings | File Templates.
 */
@Singleton(strict=false)
class Abaddon {

	private Thread deathTimer

	private def managedObjects = []

	private def managedCollections = []

	private Abaddon() {

		deathTimer = Thread.start {
			while(true) {
				def interrupted = false
				def obj = managedObjects[0]

				if (obj != null) {
					sleep(obj.@impendingDoom - System.currentTimeMillis(), {
						interrupted = true
					})
					if (!interrupted) {
						if (obj.@callback) {
							obj.@callback(obj.@delegate)
						}
						obj.@delegate = null
						managedObjects.remove(0)

						println 'Deleted object'
					}
					interrupted = false
				}
				else {
					sleep 10
				}
			}
		}
	}

	synchronized def registerObject(def object, long lifespan, Closure callback = null) {
		long impendingDoom = System.currentTimeMillis() + lifespan

		def obj = new MortalObject(object, impendingDoom, callback)
		managedObjects.add(calculateInsertionIndex(impendingDoom), obj)

		deathTimer.interrupt()

		return obj
	}

	def registerCollection(Collection collection) {
		DelegatingObject obj = new DelegatingObject(delegate: collection)

		println collection.class

		obj.@preInvoke = { name, args ->
			obj.@delegate.removeAll { it.@delegate == null }
		}

		return obj
	}

	private int calculateInsertionIndex(long impendingDoom, int left = 0, int right = managedObjects.size()) {
		if (left < right) {
			int index = (right + left) / 2
			if (managedObjects[index].@impendingDoom < impendingDoom) {
				return calculateInsertionIndex(impendingDoom, index + 1, right)
			}
			else {
				return calculateInsertionIndex(impendingDoom, left, index - 1)
			}
		}
		else {
			return right // its actually smaller potentially
		}
	}

	public static void main(String[] args) {

		def list = []
		list = Abaddon.instance.registerCollection(list)

		list << Abaddon.instance.registerObject('1', 1000) { println it }
		list << Abaddon.instance.registerObject('2', 3400) { println it }
		list << Abaddon.instance.registerObject('3', 1400) { println it }

		list.add(0, Abaddon.instance.registerObject('4', 3000, {println it}))

		while (true) {
			println( list.collect { return it } )
			sleep 100
		}
	}
}
