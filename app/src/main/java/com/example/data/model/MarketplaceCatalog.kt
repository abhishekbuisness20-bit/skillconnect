package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

data class ServiceCategoryInfo(
    val id: String,
    val nameEn: String,
    val nameHi: String,
    val subcategories: List<String>,
    val icon: ImageVector
)

data class FaqItem(
    val questionEn: String,
    val answerEn: String,
    val questionHi: String,
    val answerHi: String
)

data class SampleTestimonial(
    val quote: String,
    val author: String,
    val city: String,
    val serviceUsed: String
)

data class LegalPolicySection(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val summary: String,
    val clauses: List<String>
)

object MarketplaceCatalog {

    val categories: List<ServiceCategoryInfo> = listOf(
        ServiceCategoryInfo("Graphic Design", "Graphic Design", "ग्राफ़िक डिज़ाइन", listOf("Social Media Graphics", "Brochures & Flyers", "Packaging Design", "UI Illustration"), Icons.Default.Brush),
        ServiceCategoryInfo("Logo Design", "Logo Design", "लोगो डिज़ाइन", listOf("Minimalist Logo", "Brand Identity Kit", "3D & Mascot Logo", "Typography Logo"), Icons.Default.DesignServices),
        ServiceCategoryInfo("Video Editing", "Video Editing", "वीडियो एडिटिंग", listOf("YouTube & Reels Editing", "Corporate Promo", "Wedding Highlights", "Color Grading"), Icons.Default.Videocam),
        ServiceCategoryInfo("Website Development", "Website Development", "वेबसाइट डेवलपमेंट", listOf("Business Website", "E-Commerce Store", "Landing Page", "Full-Stack Web App"), Icons.Default.Code),
        ServiceCategoryInfo("Photography", "Photography", "फ़ोटोग्राफ़ी", listOf("Event Photography", "Product Shoot", "Portrait & Studio", "Real Estate Shoot"), Icons.Default.CameraAlt),
        ServiceCategoryInfo("Digital Marketing", "Digital Marketing", "डिजिटल मार्केटिंग", listOf("SEO Audit", "Google & Meta Ads", "Social Media Management", "Local Business SEO"), Icons.Default.TrendingUp),
        ServiceCategoryInfo("Home Cleaning", "Home Cleaning", "होम क्लीनिंग", listOf("Deep Home Cleaning", "Kitchen & Bathroom", "Sofa & Carpet", "Move-in Cleaning"), Icons.Default.CleaningServices),
        ServiceCategoryInfo("Electrician", "Electrician", "इलेक्ट्रीशियन", listOf("Wiring & MCB", "Fan & Light Fitting", "Inverter & UPS Setup", "Appliance Points"), Icons.Default.ElectricalServices),
        ServiceCategoryInfo("Plumbing", "Plumbing", "प्लंबिंग", listOf("Tap & Pipe Repair", "Water Tank & Motor", "Bathroom Fittings", "Drainage Blockage"), Icons.Default.Plumbing),
        ServiceCategoryInfo("AC Repair", "AC Repair", "एसी रिपेयर और सर्विस", listOf("Split/Window AC Service", "Gas Refill", "AC Installation", "PCB & Cooling Repair"), Icons.Default.AcUnit),
        ServiceCategoryInfo("Tutoring", "Tutoring", "ट्यूशन और कोचिंग", listOf("Mathematics & Science", "Spoken English & Hindi", "Coding & Computer Basics", "Board Exam Prep"), Icons.Default.School),
        ServiceCategoryInfo("Computer Repair", "Computer Repair", "कंप्यूटर और लैपटॉप रिपेयर", listOf("OS & Software Setup", "SSD & RAM Upgrade", "Screen & Keyboard", "Wi-Fi & Printer Setup"), Icons.Default.Computer),
        ServiceCategoryInfo("Beauty Services", "Beauty Services", "ब्यूटी और ग्रूमिंग", listOf("Bridal & Party Makeup", "Skincare & Facial", "Hair Styling", "Mehendi Art"), Icons.Default.Face),
        ServiceCategoryInfo("Event Services", "Event Services", "इवेंट सर्विसेज़", listOf("Birthday & Decor", "Catering Coordination", "Sound & Lighting", "Corporate Events"), Icons.Default.Celebration),
        ServiceCategoryInfo("Other Local Services", "Other Local Services", "अन्य लोकल सेवाएं", listOf("Carpentry", "Painting", "Packers & Movers", "Appliance Repair"), Icons.Default.Handshake)
    )

    val indianCities: Map<String, List<String>>
        get() = IndiaLocationDirectory.indianCitiesMap

    val valuePropositionsEn = listOf(
        "Direct Customer–Provider Connection" to "Compare portfolios, transparent pricing models, and verified badges without middlemen.",
        "Genuine Reviews Only" to "Reviews can only be submitted by customers after a booking is confirmed completed.",
        "Built for Indian Cities & Localities" to "Discover both digital freelancers and neighbourhood home-service specialists across India.",
        "Structured Enquiry & Status Tracking" to "Every enquiry is tracked from Pending to Accepted, In Progress, and Completed."
    )

    val valuePropositionsHi = listOf(
        "सीधा ग्राहक–प्रोफेशनल संपर्क" to "बिना किसी बिचौलिए के पोर्टफोलियो, पारदर्शी कीमत और वेरिफाइड बैज की तुलना करें।",
        "केवल प्रामाणिक रिव्यू" to "केवल काम पूरा होने की पुष्टि के बाद ही ग्राहक रिव्यू और रेटिंग दे सकते हैं।",
        "भारतीय शहरों और लोकल एरिया के लिए निर्मित" to "डिजिटल फ्रीलांसर और अपने क्षेत्र के भरोसेमंद लोकल प्रोफेशनल्स को आसानी से खोजें।",
        "स्पष्ट बुकिंग और स्टेटस ट्रैकिंग" to "पेंडिंग से लेकर एक्सेप्टेड, इन-प्रोग्रेस और कंप्लीटेड तक हर रिक्वेस्ट को ट्रैक करें।"
    )

    val howItWorksStepsEn = listOf(
        "Step 1: Search for a service" to "Filter by skill category, Indian city/locality, budget, pricing model, and verification status.",
        "Step 2: Compare professionals & portfolios" to "Inspect scope, deliverables, turnaround time, and genuine completed-job reviews.",
        "Step 3: Send an enquiry or booking request" to "Specify your project requirements, preferred date, and request either a quote or booking.",
        "Step 4: Track & review after completion" to "Coordinate via in-app messages, track progress, and leave an honest 1–5 star review once completed."
    )

    val howItWorksStepsHi = listOf(
        "चरण 1: सेवा खोजें" to "कैटेगरी, शहर/लोकैलिटी, बजट और वेरिफाइड प्रोफेशनल्स के अनुसार फ़िल्टर करें।",
        "चरण 2: प्रोफेशनल्स और पोर्टफोलियो की तुलना करें" to "काम का विवरण, डिलीवरेबल्स, समय-सीमा और असली ग्राहकों के रिव्यू देखें।",
        "चरण 3: बुकिंग या कोटेशन रिक्वेस्ट भेजें" to "अपनी ज़रूरतें और पसंदीदा तारीख लिखकर प्रोवाइडर को रिक्वेस्ट भेजें।",
        "चरण 4: ट्रैक करें और काम पूरा होने पर रिव्यू दें" to "इन-ऐप चैट से बात करें और काम पूरा होने के बाद ईमानदार रिव्यू साझा करें।"
    )

    // Clearly labelled development sample testimonials as mandated by Section 5
    val sampleTestimonials = listOf(
        SampleTestimonial(
            quote = "SkillConnect helped our D2C tea brand find a logo & packaging designer in Bengaluru with clear deliverables and zero guesswork.",
            author = "Ananya Deshmukh (Sample Preview)",
            city = "Bengaluru",
            serviceUsed = "Logo Design & Packaging"
        ),
        SampleTestimonial(
            quote = "Finding a reliable electrician in Pune for our studio setup was straightforward—being able to message directly before acceptance saved time.",
            author = "Siddharth Kulkarni (Sample Preview)",
            city = "Pune",
            serviceUsed = "Electrician Services"
        ),
        SampleTestimonial(
            quote = "As an independent video editor in Mumbai, my SkillConnect portfolio lets local businesses request quotes with clear project scopes.",
            author = "Kabir Mehta (Sample Preview)",
            city = "Mumbai",
            serviceUsed = "Video Editing Provider"
        )
    )

    val faqs: List<FaqItem> = listOf(
        FaqItem(
            questionEn = "How does registration and authentication work on SkillConnect?",
            answerEn = "SkillConnect uses secure Google Sign-In via Android Credential Manager. During onboarding, you select whether you are joining as a Customer or Service Provider, choose your city and preferred language (English or Hindi), and can switch roles anytime from your profile.",
            questionHi = "SkillConnect पर रजिस्ट्रेशन और लॉगिन कैसे काम करता है?",
            answerHi = "SkillConnect सुरक्षित Google Sign-In का उपयोग करता है। साइन-इन के बाद आप ग्राहक या सर्विस प्रोवाइडर की भूमिका, अपना शहर और भाषा (अंग्रेज़ी या हिन्दी) चुन सकते हैं।"
        ),
        FaqItem(
            questionEn = "Is an enquiry or booking request immediately confirmed?",
            answerEn = "No. Submitting a booking request or quote enquiry creates a 'Pending' request. A booking is only confirmed once the service provider reviews your requirements and explicitly marks the request as 'Accepted'.",
            questionHi = "क्या बुकिंग रिक्वेस्ट भेजते ही बुकिंग कन्फर्म हो जाती है?",
            answerHi = "नहीं। रिक्वेस्ट भेजने पर वह 'Pending' स्थिति में रहती है। जब प्रोवाइडर आपकी आवश्यकताओं को देखकर उसे 'Accept' करता है, तभी बुकिंग कन्फर्म मानी जाती है।"
        ),
        FaqItem(
            questionEn = "How are service prices and payments handled?",
            answerEn = "Providers list services under Fixed Price, Hourly Rate, or Request a Quote models in INR (₹). Platform payment processing is currently in transparent Demo Mode while direct customer-provider coordination is supported.",
            questionHi = "सेवा की कीमतें और भुगतान कैसे तय होते हैं?",
            answerHi = "प्रोवाइडर अपनी सेवाओं के लिए फिक्स्ड प्राइस, प्रति घंटा दर या कोटेशन मॉडल (₹ में) चुनते हैं।"
        ),
        FaqItem(
            questionEn = "What does the Verified Provider badge mean?",
            answerEn = "A Verification Badge is displayed only when a provider submits their professional details in the Provider Verification workflow and an administrator approves the submission. It does not represent government certification.",
            questionHi = "वेरिफाइड प्रोवाइडर बैज का क्या अर्थ है?",
            answerHi = "यह बैज केवल तभी दिखता है जब प्रोवाइडर अपने व्यावसायिक विवरण सबमिट करता है और एडमिन द्वारा उसकी समीक्षा कर उसे स्वीकृत किया जाता है।"
        ),
        FaqItem(
            questionEn = "What is the cancellation and dispute policy?",
            answerEn = "Customers can cancel a 'Draft' or 'Pending' request at any time, or cancel an 'Accepted' request before work starts. If an issue arises during 'In Progress' work, either party can flag the booking as 'Disputed' and open a linked Support Ticket.",
            questionHi = "बुकिंग रद्दीकरण (Cancellation) और विवाद (Dispute) की क्या नीति है?",
            answerHi = "ग्राहक 'Pending' रिक्वेस्ट को कभी भी रद्द कर सकते हैं। यदि काम के दौरान कोई समस्या आती है, तो आप बुकिंग को 'Disputed' चिह्नित कर सपोर्ट टिकट दर्ज कर सकते हैं।"
        ),
        FaqItem(
            questionEn = "How are my personal details and privacy protected?",
            answerEn = "Private email addresses, home addresses, and verification submissions are never displayed on public profiles. Public profiles only show your display name, city/locality, skills, and optional contact details you explicitly consent to share.",
            questionHi = "मेरी निजी जानकारी और गोपनीयता की सुरक्षा कैसे की जाती है?",
            answerHi = "निजी ईमेल, घर का पता और वेरिफिकेशन विवरण कभी भी सार्वजनिक प्रोफ़ाइल पर नहीं दिखाए जाते।"
        )
    )

    val legalPolicies: List<LegalPolicySection> = listOf(
        LegalPolicySection(
            id = "privacy",
            titleEn = "Privacy Policy (Editable Template)",
            titleHi = "गोपनीयता नीति (Privacy Policy)",
            summary = "Explains how SkillConnect collects, isolates, and protects user and provider data.",
            clauses = listOf(
                "1. Data Minimization & PII Isolation: Public provider profiles display only professional display names, headlines, skills, service cities/localities, and optional user-consented contact numbers.",
                "2. Verification Documents: Information submitted through the Provider Verification workflow is stored in an isolated collection accessible strictly to the submitting user and authorized platform administrators.",
                "3. User Controls: Users may update notification preferences, toggle public contact visibility, export their profile summary, or submit an account deletion request at any time from Settings.",
                "4. Notice: This policy template must be reviewed by qualified legal counsel before commercial public launch."
            )
        ),
        LegalPolicySection(
            id = "terms",
            titleEn = "Terms of Service (Editable Template)",
            titleHi = "सेवा की शर्तें (Terms of Service)",
            summary = "Governs marketplace usage by customers and independent service providers.",
            clauses = listOf(
                "1. Marketplace Role: SkillConnect is a discovery and enquiry platform connecting customers with independent service providers and freelancers across India.",
                "2. No Guaranteed Employment or Outcome: SkillConnect does not guarantee employment, minimum earnings for providers, or specific service outcomes unless separately contracted.",
                "3. Booking Confirmation: Submitting an enquiry or booking request does not constitute a binding booking until accepted by the service provider.",
                "4. Authentic Representation: Users must not publish services under another person's identity or fabricate ratings and reviews."
            )
        ),
        LegalPolicySection(
            id = "cancellation",
            titleEn = "Cancellation & Refund Policy",
            titleHi = "रद्दीकरण और रिफंड नीति",
            summary = "Transparent rules for cancelling enquiries, bookings, and resolving disputes.",
            clauses = listOf(
                "1. Pending Enquiries: Customers may cancel any Draft or Pending booking request immediately without penalty.",
                "2. Accepted Bookings: If a customer or provider needs to cancel an Accepted booking before work commences, they must provide a clear status note through the booking tracker.",
                "3. Disputed Jobs: If work is already In Progress and a disagreement occurs regarding deliverables, either party may mark the booking as Disputed and file a Support Ticket with the booking ID.",
                "4. Demo Payment Mode: While online payment gateway integration is in Demo Mode, no platform charges are collected automatically."
            )
        ),
        LegalPolicySection(
            id = "guidelines",
            titleEn = "Provider Guidelines & Community Standards",
            titleHi = "प्रोवाइडर दिशानिर्देश और कम्युनिटी मानक",
            summary = "Standards for professional conduct, accurate pricing, and safety across India.",
            clauses = listOf(
                "1. Transparent Pricing & Scope: Clearly state whether your listing is Fixed Price, Hourly, or Request a Quote, and specify exact deliverables and turnaround times.",
                "2. Respect & Safety: Communicate exclusively through respectful, professional messages. Harassment, spam, or off-platform fraud leads to immediate account suspension.",
                "3. Review Integrity: Only customers with confirmed completed bookings may leave a review. Attempting to manipulate ratings or post duplicate reviews is blocked by platform security rules.",
                "4. Safety First: For in-person home services (Electrician, Plumbing, AC Repair, Cleaning), confirm appointment timings in-app and adhere to local safety protocols."
            )
        )
    )

    val jobOpportunities: List<JobOpportunityItem> = listOf(
        JobOpportunityItem(
            id = "job_blr_brand_kit",
            title = "D2C Packaging & Brand Identity Designer",
            companyOrClient = "Kaveri Organics Pvt. Ltd.",
            category = "Logo Design",
            city = "Bengaluru",
            payoutRangeInr = "₹18,000 – ₹28,000",
            commitmentType = "Project Milestone",
            skillsRequired = listOf("Brand Identity Kit", "Packaging Design", "Illustrator"),
            summary = "Design 4 sustainable tea pouch labels and a cohesive brand guideline book for retail launch across South India.",
            verifiedClient = true
        ),
        JobOpportunityItem(
            id = "job_mum_reels_editor",
            title = "Short-Form Reels & YouTube Podcast Video Editor",
            companyOrClient = "CreatorPulse Studio Mumbai",
            category = "Video Editing",
            city = "Mumbai",
            payoutRangeInr = "₹25,000 / month",
            commitmentType = "Part-Time Contract",
            skillsRequired = listOf("YouTube & Reels Editing", "Color Grading", "Motion Captions"),
            summary = "Edit 12 high-retention founder podcast clips and 4 YouTube episodes per month with bilingual English/Hindi captions.",
            verifiedClient = true
        ),
        JobOpportunityItem(
            id = "job_del_commercial_elec",
            title = "Co-Working Office Electrical & UPS Setup Specialist",
            companyOrClient = "Noida WorkHub Spaces",
            category = "Electrician",
            city = "Delhi NCR",
            payoutRangeInr = "₹12,500 – ₹16,000",
            commitmentType = "Immediate Gig",
            skillsRequired = listOf("Wiring & MCB", "Inverter & UPS Setup", "Safety Audit"),
            summary = "Complete 24 workstation power distribution panels, MCB load balancing, and 10kVA online UPS wiring in Sector 62.",
            verifiedClient = true
        ),
        JobOpportunityItem(
            id = "job_hyd_shopify_web",
            title = "Full-Stack E-Commerce Store & UPI Checkout Integration",
            companyOrClient = "Deccan Craft Collective",
            category = "Website Development",
            city = "Hyderabad",
            payoutRangeInr = "₹32,000 – ₹45,000",
            commitmentType = "Project Milestone",
            skillsRequired = listOf("E-Commerce Store", "Landing Page", "Speed Optimization"),
            summary = "Build a mobile-first handloom catalogue store with GST invoice generation and WhatsApp order status updates.",
            verifiedClient = true
        ),
        JobOpportunityItem(
            id = "job_pun_ac_maintenance",
            title = "Annual Split AC Maintenance Partner (18 Units)",
            companyOrClient = "Koregaon Boutique Suites",
            category = "AC Repair",
            city = "Pune",
            payoutRangeInr = "₹14,400",
            commitmentType = "Immediate Gig",
            skillsRequired = listOf("Split/Window AC Service", "Gas Refill", "PCB & Cooling Repair"),
            summary = "Deep jet-pump servicing, coil sanitization, and pressure checks for 18 inverter split ACs before peak season.",
            verifiedClient = true
        )
    )

    val skillCourses: List<SkillCourseItem> = listOf(
        SkillCourseItem(
            id = "crs_verified_pro_mastery",
            title = "SkillConnect Verified Provider Excellence & Client Trust",
            instructorName = "SkillConnect Academy India",
            category = "Digital Marketing",
            durationHours = 4,
            level = "Beginner",
            priceInr = 0L,
            badgeEarned = "Client Trust Certified",
            summary = "Master scoping, transparent INR quoting, milestone communication, and winning repeat local bookings."
        ),
        SkillCourseItem(
            id = "crs_d2c_brand_packaging",
            title = "Commercial Brand Packaging & Print-Ready Design Systems",
            instructorName = "Meera Nair • Senior Art Director",
            category = "Graphic Design",
            durationHours = 12,
            level = "Intermediate",
            priceInr = 799L,
            badgeEarned = "Print & Brand Specialist",
            summary = "Learn dielines, CMYK prepress standards, FSSAI label compliance, and high-converting Indian retail packaging."
        ),
        SkillCourseItem(
            id = "crs_smart_home_electrical",
            title = "Modern Inverter, Smart MCB & Home Safety Standards",
            instructorName = "Rajeshwar Rao • Master Technician",
            category = "Electrician",
            durationHours = 8,
            level = "Pro Certification",
            priceInr = 499L,
            badgeEarned = "Safety Protocol Pro",
            summary = "Hands-on safety protocols, earthing diagnostics, surge protection, and clean residential/office installation practices."
        ),
        SkillCourseItem(
            id = "crs_high_retention_video",
            title = "High-Retention Hindi & English Reels Storytelling",
            instructorName = "Aarav Khanna • Creator Strategist",
            category = "Video Editing",
            durationHours = 10,
            level = "Intermediate",
            priceInr = 699L,
            badgeEarned = "Retention Editor Badge",
            summary = "Audio mastering, dynamic bilingual typography, hook pacing, and colour workflows for Indian brands and creators."
        )
    )
}

