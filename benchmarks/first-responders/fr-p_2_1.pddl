(define (problem FR_2_1)
 (:domain first-response)
 (:objects  l1 l2  - location
	    f1 f2 - fire_unit
	    v1 - victim
	    m1 m2 - medical_unit
)
 (:init
        (adjacent l1 l1)
        (adjacent l2 l2)
        (fire l1)
        (fire-unit-at f1 l2)
        (fire-unit-at f2 l2)
        (hospital l1)
        (medical-unit-at m1 l1)
        (medical-unit-at m2 l1)
        (nfire l2)
        (victim-at v1 l2)
        (victim-status v1 dying)
        (water-at l1)
        (water-at l2)
	)
 (:goal (and  (nfire l1)  (victim-status v1 healthy)))
 )
