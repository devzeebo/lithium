package com.zeebo.lithium.mortalis

import java.util.concurrent.locks.ReentrantLock

/**
 * User: Eric Siebeneich
 * Date: 9/12/14
 */
class Abaddon {

	private static Thread deathTimer

	private static def managedObjects = []

	private static ReentrantLock mutex = new ReentrantLock()

	static {

		deathTimer = Thread.startDaemon {
			while (true) {
				def interrupted = false
				def obj = managedObjects[0]

				if (obj != null) {
					sleep(obj.@impendingDoom - System.currentTimeMillis(), {
						interrupted = true
					})
					if (!interrupted) {
						withMutex {
							if (obj.@callback) {
								obj.@callback(obj.@delegate)
							}
							obj.@delegate = null
							managedObjects.remove(0)
						}
					}
					interrupted = false
				} else {
					sleep 10
				}
			}
		}
	}

	synchronized static def registerObject(def object, long lifespan, Closure callback = null) {
		long impendingDoom = System.currentTimeMillis() + lifespan

		withMutex {
			def obj = new MortalObject(object, impendingDoom, callback)
			managedObjects.add(calculateInsertionIndex(impendingDoom), obj)

			deathTimer.interrupt()

			return obj
		}
	}

	static def registerCollection(Collection collection) {
		DelegatingObject obj = new DelegatingObject(delegate: collection)

		obj.@preInvoke = { name, args ->
			obj.@delegate.removeAll { try { it.@delegate == null } catch (MissingFieldException mfe) {} }
		}

		return obj
	}

	static def registerMap(Map map) {
		DelegatingObject obj = new DelegatingObject(delegate: map)

		obj.@preInvoke = { name, args ->
			obj.@delegate.keySet().removeAll { try { it.@delegate == null } catch (MissingFieldException mfe) {} }
			obj.@delegate.values().removeAll { try { it.@delegate == null } catch (MissingFieldException mfe) {} }
		}

		return obj
	}

	private static int calculateInsertionIndex(long impendingDoom, int left = 0, int right = managedObjects.size()) {
		if (left < right) {
			int index = (right + left) / 2
			if (managedObjects[index].@impendingDoom < impendingDoom) {
				return calculateInsertionIndex(impendingDoom, index + 1, right)
			} else {
				return calculateInsertionIndex(impendingDoom, left, index - 1)
			}
		} else {
			return right // its actually smaller potentially
		}
	}

	private static def withMutex(Closure closure) {
		mutex.lock()

		def result
		try {
			result = closure()
		}
		finally {
			mutex.unlock()
		}

		return result
	}

	public static void main(String[] args) {

		def list = [:]

		list = Abaddon.registerMap(list)

		list['1'] = Abaddon.registerObject('1', 1000) { println it }
		list['2'] = Abaddon.registerObject('2', 3400) { println it }
		list['3'] = Abaddon.registerObject('3', 1400) { println it }
		list['8'] = Abaddon.registerObject('8', -100) { println it }

		list['test'] = 'test'

		while (list.size() != 1) {
			println(list.collect { k, v -> return "$k: $v" })
			sleep 100
		}
	}
}
