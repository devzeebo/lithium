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

		def obj = new MortalObject(impendingDoom: impendingDoom, delegate: object, callback: callback)
		managedObjects.add(calculateInsertionIndex(impendingDoom), obj)

		return obj
	}

	private int calculateInsertionIndex(long impendingDoom, int left = 0, int right = managedObjects.size()) {
		if (left < right) {
			int index = (right - left) / 2
			if (managedObjects[index].@impendingDoom < impendingDoom) {
				return calculateInsertionIndex(impendingDoom, index, right)
			}
			else {
				return calculateInsertionIndex(impendingDoom, left, index)
			}
		}
		else {
			return left
		}
	}

	public static void main(String[] args) {
		Abaddon.instance
		sleep 100

		Abaddon.instance.registerObject('Hello World!', 3000) {
			println it.toUpperCase()
		}
	}
}
