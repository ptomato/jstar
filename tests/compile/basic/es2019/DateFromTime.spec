          = DayWithinYear(_t_) + 1  if MonthFromTime(_t_) = 0
          = DayWithinYear(_t_) - 30  if MonthFromTime(_t_) = 1
          = DayWithinYear(_t_) - 58 - InLeapYear(_t_) if MonthFromTime(_t_) = 2
          = DayWithinYear(_t_) - 89 - InLeapYear(_t_) if MonthFromTime(_t_) = 3
          = DayWithinYear(_t_) - 119 - InLeapYear(_t_) if MonthFromTime(_t_) = 4
          = DayWithinYear(_t_) - 150 - InLeapYear(_t_) if MonthFromTime(_t_) = 5
          = DayWithinYear(_t_) - 180 - InLeapYear(_t_) if MonthFromTime(_t_) = 6
          = DayWithinYear(_t_) - 211 - InLeapYear(_t_) if MonthFromTime(_t_) = 7
          = DayWithinYear(_t_) - 242 - InLeapYear(_t_) if MonthFromTime(_t_) = 8
          = DayWithinYear(_t_) - 272 - InLeapYear(_t_) if MonthFromTime(_t_) = 9
          = DayWithinYear(_t_) - 303 - InLeapYear(_t_) if MonthFromTime(_t_) = 10
          = DayWithinYear(_t_) - 333 - InLeapYear(_t_) if MonthFromTime(_t_) = 11