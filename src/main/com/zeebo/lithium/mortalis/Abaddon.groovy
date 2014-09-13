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

		def obj = new MortalObject(impendingDoom: impendingDoom, delegate: object, callback: callback)
		managedObjects.add(calculateInsertionIndex(impendingDoom), obj)

		deathTimer.interrupt()

		return obj
	}

	void registerList(List collection) {
		collection.metaClass.clean = {
			collection.retainAll { it?.@delegate != null }
		}
		def getProperty = collection.class.metaClass.getMetaMethod('getProperty', [String] as Class[])
		collection.metaClass.getProperty = { String prop ->
			println 'GET PROPERTY'
			collection.clean()
			return getProperty.invoke(collection, prop)
		}
		collection.metaClass.getProperty = { String prop ->
			println 'GET PROPERTY'
			collection.clean()
			return getProperty.invoke(collection, prop)
		}

		managedCollections << collection
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

	}
}
