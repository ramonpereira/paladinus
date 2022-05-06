(define (problem FR_1_3)
 (:domain first-response)
 (:objects  l1  - location
	    f1 - fire_unit
	    v1 v2 v3 - victim
	    m1 - medical_unit
)
 (:init
        (adjacent l1 l1)
        (fire l1)
        (fire-unit-at f1 l1)
        (hospital l1)
        (medical-unit-at m1 l1)
        (victim-at v1 l1)
        (victim-at v2 l1)
        (victim-at v3 l1)
        (victim-status v1 hurt)
        (victim-status v2 hurt)
        (victim-status v3 dying)
        (water-at l1)
	)
 (:goal (and  (nfire l1) (nfire l1) (nfire l1)  (victim-status v1 healthy) (victim-status v2 healthy) (victim-status v3 healthy)))
 )
