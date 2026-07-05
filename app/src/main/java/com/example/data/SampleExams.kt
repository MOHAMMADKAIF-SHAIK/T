package com.example.data

object SampleExams {
    val examsList: List<MockExam> by lazy {
        listOf(
            MockExam(
                id = "sample_jee_physics",
                title = "IIT JEE Physics 2024 (Mechanics)",
                category = "Physics",
                timeLimitMinutes = 15,
                questionsJson = MoshiUtils.serializeQuestions(
                    listOf(
                        Question(
                            id = "jee_q1",
                            questionText = "A block of mass M is placed on a smooth wedge of inclination θ which is placed on a smooth horizontal floor. What horizontal acceleration 'a' must be given to the wedge so that the block remains stationary relative to the wedge?",
                            options = listOf(
                                "A) a = g sin θ",
                                "B) a = g cos θ",
                                "C) a = g tan θ",
                                "D) a = g cot θ"
                            ),
                            correctAnswer = "C",
                            explanation = "For the block to remain stationary relative to the wedge, the net force along the inclined plane must be zero. If the wedge accelerates to the right with acceleration 'a', a pseudo-force 'Ma' acts on the block to the left in the wedge's frame. Resolving forces along the incline:\n\n- Component of gravity down the incline: Mg sin θ\n- Component of pseudo-force up the incline: Ma cos θ\n\nFor no relative motion:\nMg sin θ = Ma cos θ\na = g (sin θ / cos θ) = g tan θ.\nTherefore, Option C is correct.",
                            category = "Mechanics",
                            difficulty = "Hard"
                        ),
                        Question(
                            id = "jee_q2",
                            questionText = "A particle is projected from the ground with a speed u at an angle θ with the horizontal. What is the radius of curvature of its trajectory at the highest point of its motion?",
                            options = listOf(
                                "A) R = u² / g",
                                "B) R = (u² cos² θ) / g",
                                "C) R = (u² sin² θ) / g",
                                "D) R = u² sin(2θ) / g"
                            ),
                            correctAnswer = "B",
                            explanation = "At the highest point of projectile motion:\n\n1. The velocity of the particle is entirely horizontal, v = u cos θ.\n2. The acceleration is entirely vertical downwards and equals gravity, a = g.\n\nThe centripetal acceleration is a_c = v² / R, where R is the radius of curvature.\nHere, the acceleration perpendicular to the velocity is g.\nTherefore:\ng = (u cos θ)² / R\nR = (u² cos² θ) / g.\nThus, Option B is correct.",
                            category = "Projectiles",
                            difficulty = "Medium"
                        ),
                        Question(
                            id = "jee_q3",
                            questionText = "A uniform solid cylinder of mass M and radius R rolls down an inclined plane of angle θ without slipping. The acceleration of the cylinder is:",
                            options = listOf(
                                "A) a = g sin θ",
                                "B) a = (1/2) g sin θ",
                                "C) a = (2/3) g sin θ",
                                "D) a = (5/7) g sin θ"
                            ),
                            correctAnswer = "C",
                            explanation = "For a solid cylinder rolling down an incline of inclination θ:\n\n1. Equation of motion down the plane: Mg sin θ - f = Ma, where f is static friction.\n2. Torque equation about center of mass: f * R = I * α, where I = (1/2) M R².\n3. For rolling without slipping: α = a / R.\n\nSubstituting α:\nf * R = (1/2) M R² * (a / R)  ==> f = (1/2) M a.\n\nNow substitute f into the motion equation:\nMg sin θ - (1/2) Ma = Ma\nMg sin θ = (3/2) Ma\na = (2/3) g sin θ.\nHence, Option C is correct.",
                            category = "Rotational Dynamics",
                            difficulty = "Hard"
                        )
                    )
                )
            ),
            MockExam(
                id = "sample_upsc_polity",
                title = "UPSC Indian Polity Quiz",
                category = "Civics",
                timeLimitMinutes = 10,
                questionsJson = MoshiUtils.serializeQuestions(
                    listOf(
                        Question(
                            id = "upsc_q1",
                            questionText = "The power to decide the question of disqualification of a member of a State Legislature on the grounds of defection lies with:",
                            options = listOf(
                                "A) The Governor of the State",
                                "B) The President of India",
                                "C) The Speaker or Chairperson of the House",
                                "D) The High Court of the State"
                            ),
                            correctAnswer = "C",
                            explanation = "Under the Tenth Schedule (Anti-Defection Law) of the Constitution of India, the decision on questions of disqualification on grounds of defection is made by the Speaker or Chairperson of the respective House of Parliament or State Legislature. While this decision is subject to judicial review (Kihoto Hollohan case), the primary deciding authority is the presiding officer. Therefore, Option C is correct.",
                            category = "Legislature",
                            difficulty = "Medium"
                        ),
                        Question(
                            id = "upsc_q2",
                            questionText = "Which Article of the Constitution of India safeguards one's right to marry the person of one's choice?",
                            options = listOf(
                                "A) Article 19",
                                "B) Article 21",
                                "C) Article 25",
                                "D) Article 29"
                            ),
                            correctAnswer = "B",
                            explanation = "The Supreme Court of India in several judgments, including the landmark Hadiya Case (Shafin Jahan v. Asokan K.M., 2018), held that the right to marry a person of one's choice is an integral part of Article 21 (Right to Life and Personal Liberty) of the Constitution of India. It forms the core of personal autonomy, privacy, and dignity. Option B is correct.",
                            category = "Fundamental Rights",
                            difficulty = "Easy"
                        ),
                        Question(
                            id = "upsc_q3",
                            questionText = "Consider the following statements regarding the Writ of Habeas Corpus:\n1. It can be issued against both public authorities and private individuals.\n2. It cannot be issued where the detention is lawful.\nWhich of the statements given above is/are correct?",
                            options = listOf(
                                "A) 1 only",
                                "B) 2 only",
                                "C) Both 1 and 2",
                                "D) Neither 1 nor 2"
                            ),
                            correctAnswer = "C",
                            explanation = "Statement 1 is correct: Unlike other writs like Mandamus, Certiorari, and Prohibition, Habeas Corpus can be issued against public officials as well as private individuals who have unlawfully detained another person.\n\nStatement 2 is correct: Habeas Corpus is a remedy against unlawful detention. If the detention is lawful, or is by a competent court, or for contempt of legislature/court, the writ cannot be issued.\n\nSince both statements are correct, Option C is the correct answer.",
                            category = "Judiciary",
                            difficulty = "Hard"
                        )
                    )
                )
            ),
            MockExam(
                id = "sample_sat_math",
                title = "SAT Math Essentials",
                category = "Mathematics",
                timeLimitMinutes = 8,
                questionsJson = MoshiUtils.serializeQuestions(
                    listOf(
                        Question(
                            id = "sat_q1",
                            questionText = "If 3x + 5 = 20, what is the value of 6x - 2?",
                            options = listOf(
                                "A) 15",
                                "B) 28",
                                "C) 30",
                                "D) 38"
                            ),
                            correctAnswer = "B",
                            explanation = "First, solve the linear equation for x:\n3x + 5 = 20\n3x = 15\nx = 5.\n\nNow, substitute x = 5 into the expression 6x - 2:\n6(5) - 2 = 30 - 2 = 28.\nThus, the correct answer is Option B.",
                            category = "Algebra",
                            difficulty = "Easy"
                        ),
                        Question(
                            id = "sat_q2",
                            questionText = "A line in the xy-plane passes through the origin and has a slope of 1/3. Which of the following points lies on the line?",
                            options = listOf(
                                "A) (1, 3)",
                                "B) (3, 1)",
                                "C) (3, 3)",
                                "D) (6, 3)"
                            ),
                            correctAnswer = "B",
                            explanation = "The equation of a line passing through the origin is given by y = mx, where m is the slope.\nHere, m = 1/3, so the equation of the line is:\ny = (1/3) x  ==>  x = 3y.\n\nLet's test each point:\n- For (1, 3): y = 3, but (1/3)(1) = 1/3 != 3 (No)\n- For (3, 1): y = 1, and (1/3)(3) = 1 == 1 (Yes!)\n- For (3, 3): y = 3, but (1/3)(3) = 1 != 3 (No)\n- For (6, 3): y = 3, but (1/3)(6) = 2 != 3 (No)\n\nTherefore, the point (3, 1) lies on the line, making Option B the correct choice.",
                            category = "Coordinate Geometry",
                            difficulty = "Medium"
                        )
                    )
                )
            )
        )
    }
}
