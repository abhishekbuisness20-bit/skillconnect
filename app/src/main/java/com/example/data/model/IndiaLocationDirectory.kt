package com.example.data.model

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

data class IndianCityEntry(
    val name: String,
    val state: String,
    val stateCode: String,
    val isUnionTerritory: Boolean = false,
    val pinPrefix: String,
    val defaultPin: String,
    val latitude: Double,
    val longitude: Double,
    val localities: List<String>
)

data class IndianStateEntry(
    val name: String,
    val code: String,
    val isUnionTerritory: Boolean = false,
    val cities: List<IndianCityEntry>
)

data class ResolvedIndiaLocation(
    val state: String,
    val city: String,
    val locality: String,
    val pinCode: String,
    val postOffices: List<String> = emptyList(),
    val source: String = "Directory"
)

object IndiaLocationDirectory {

    val allStatesAndUTs: List<IndianStateEntry> = listOf(
        // ==================== 28 STATES ====================
        IndianStateEntry(
            name = "Andhra Pradesh",
            code = "AP",
            cities = listOf(
                IndianCityEntry("Visakhapatnam", "Andhra Pradesh", "AP", false, "530", "530001", 17.6868, 83.2185, listOf("MVP Colony", "Madhurawada", "Dwaraka Nagar", "Gajuwaka", "Seethammadhara", "Rushikonda")),
                IndianCityEntry("Vijayawada", "Andhra Pradesh", "AP", false, "520", "520001", 16.5062, 80.6480, listOf("Benz Circle", "Governorpet", "MG Road", "Patamata", "Bhavanipuram", "Auto Nagar")),
                IndianCityEntry("Guntur", "Andhra Pradesh", "AP", false, "522", "522001", 16.3067, 80.4365, listOf("Brodipet", "Arundelpet", "Lakshmipuram", "Pattabhipuram", "Kothapet")),
                IndianCityEntry("Tirupati", "Andhra Pradesh", "AP", false, "517", "517501", 13.6288, 79.4192, listOf("Alipiri", "Air Bypass Road", "MR Palli", "Renigunta Road", "Korlagunta")),
                IndianCityEntry("Nellore", "Andhra Pradesh", "AP", false, "524", "524001", 14.4426, 79.9865, listOf("Magunta Layout", "Pogathota", "Trunk Road", "Vedayapalem")),
                IndianCityEntry("Kurnool", "Andhra Pradesh", "AP", false, "518", "518001", 15.8281, 78.0373, listOf("Nandyal Checkpost", "Budhwarpet", "Kallur", "Bellary Chowrasta")),
                IndianCityEntry("Rajahmundry", "Andhra Pradesh", "AP", false, "533", "533101", 17.0005, 81.8040, listOf("Danavaipeta", "T. Nagar", "Morampudi", "Kambala Cheruvu")),
                IndianCityEntry("Kakinada", "Andhra Pradesh", "AP", false, "533", "533001", 16.9891, 82.2475, listOf("Bhanugudi Junction", "Sarpavaram", "Suryaraopeta", "Ramanayyapeta")),
                IndianCityEntry("Anantapur", "Andhra Pradesh", "AP", false, "515", "515001", 14.6819, 77.6006, listOf("Kamalanagar", "Ram Nagar", "Srinagar Colony", "Subhash Road"))
            )
        ),
        IndianStateEntry(
            name = "Arunachal Pradesh",
            code = "AR",
            cities = listOf(
                IndianCityEntry("Itanagar", "Arunachal Pradesh", "AR", false, "791", "791111", 27.0844, 93.6053, listOf("Ganga Market", "Bank Tinali", "Naharlagun", "Chimpu", "Nirjuli")),
                IndianCityEntry("Tawang", "Arunachal Pradesh", "AR", false, "790", "790104", 27.5861, 91.8594, listOf("Old Market", "Nehru Market", "Lebrang", "Monastery Road")),
                IndianCityEntry("Pasighat", "Arunachal Pradesh", "AR", false, "791", "791102", 28.0660, 95.3263, listOf("Main Market", "Bakin Pertin Colony", "Raneghat", "High Region"))
            )
        ),
        IndianStateEntry(
            name = "Assam",
            code = "AS",
            cities = listOf(
                IndianCityEntry("Guwahati", "Assam", "AS", false, "781", "781001", 26.1445, 91.7362, listOf("GS Road", "Dispur", "Beltola", "Paltan Bazaar", "Pan Bazaar", "Six Mile", "Zoo Road", "Jalukbari")),
                IndianCityEntry("Silchar", "Assam", "AS", false, "788", "788001", 24.8333, 92.7789, listOf("Ambikapatty", "Tarapur", "Malugram", "Rangirkhari", "Meherpur")),
                IndianCityEntry("Dibrugarh", "Assam", "AS", false, "786", "786001", 27.4728, 94.9120, listOf("AT Road", "Mancotta Road", "Boiragimoth", "Graham Bazar", "Milan Nagar")),
                IndianCityEntry("Jorhat", "Assam", "AS", false, "785", "785001", 26.7509, 94.2037, listOf("Gar-Ali", "AT Road", "Tarajan", "Club Road", "Cinnamara")),
                IndianCityEntry("Tezpur", "Assam", "AS", false, "784", "784001", 26.6528, 92.7926, listOf("Mission Chariali", "Mahabhairab", "Mazgaon", "Ketekibari"))
            )
        ),
        IndianStateEntry(
            name = "Bihar",
            code = "BR",
            cities = listOf(
                IndianCityEntry("Patna", "Bihar", "BR", false, "800", "800001", 25.5941, 85.1376, listOf("Boring Road", "Kankarbagh", "Bailey Road", "Frazer Road", "Rajendra Nagar", "Patliputra Colony", "Danapur")),
                IndianCityEntry("Gaya", "Bihar", "BR", false, "823", "823001", 24.7914, 85.0002, listOf("AP Colony", "Swarajpuri Road", "Bodhgaya", "Civil Lines", "Delha")),
                IndianCityEntry("Muzaffarpur", "Bihar", "BR", false, "842", "842001", 26.1197, 85.3910, listOf("Mithanpura", "Moti Jheel", "Kalyani", "Aghoria Bazar", "Zero Mile")),
                IndianCityEntry("Bhagalpur", "Bihar", "BR", false, "812", "812001", 25.2425, 86.9842, listOf("Tilka Manjhi", "Adampur", "Khalifabagh", "Sabour", "Nathnagar")),
                IndianCityEntry("Darbhanga", "Bihar", "BR", false, "846", "846004", 26.1542, 85.8918, listOf("Laheriasarai", "Donar", "Mirzapur", "Benta", "Tower Chowk")),
                IndianCityEntry("Purnia", "Bihar", "BR", false, "854", "854301", 25.7771, 87.4753, listOf("Bhatta Bazar", "Line Bazar", "Madhubani", "Gulabbagh"))
            )
        ),
        IndianStateEntry(
            name = "Chhattisgarh",
            code = "CG",
            cities = listOf(
                IndianCityEntry("Raipur", "Chhattisgarh", "CG", false, "492", "492001", 21.2514, 81.6296, listOf("Shankar Nagar", "Pandri", "Civil Lines", "Telibandha", "Devendra Nagar", "Naya Raipur", "Tatibandh")),
                IndianCityEntry("Bhilai", "Chhattisgarh", "CG", false, "490", "490001", 21.1938, 81.3509, listOf("Nehru Nagar", "Supela", "Smriti Nagar", "Civic Centre", "Durg")),
                IndianCityEntry("Bilaspur", "Chhattisgarh", "CG", false, "495", "495001", 22.0797, 82.1409, listOf("Vyapar Vihar", "Sarkanda", "Rajendra Nagar", "Tifra", "Link Road")),
                IndianCityEntry("Korba", "Chhattisgarh", "CG", false, "495", "495677", 22.3595, 82.7501, listOf("TP Nagar", "Niharika", "Kosabadi", "Balco Nagar"))
            )
        ),
        IndianStateEntry(
            name = "Goa",
            code = "GA",
            cities = listOf(
                IndianCityEntry("Panaji", "Goa", "GA", false, "403", "403001", 15.4909, 73.8278, listOf("Miramar", "Dona Paula", "Porvorim", "Altinho", "Campal", "Caranzalem")),
                IndianCityEntry("Margao", "Goa", "GA", false, "403", "403601", 15.2832, 73.9862, listOf("Fatorda", "Borda", "Aquem", "Colva", "Navelim")),
                IndianCityEntry("Vasco da Gama", "Goa", "GA", false, "403", "403802", 15.3982, 73.8113, listOf("Chicalim", "Baina", "Dabolim", "Mangor Hill")),
                IndianCityEntry("Mapusa", "Goa", "GA", false, "403", "403507", 15.5937, 73.8142, listOf("Khorlim", "Calangute", "Candolim", "Anjuna", "Siolim"))
            )
        ),
        IndianStateEntry(
            name = "Gujarat",
            code = "GJ",
            cities = listOf(
                IndianCityEntry("Ahmedabad", "Gujarat", "GJ", false, "380", "380001", 23.0225, 72.5714, listOf("SG Highway", "Navrangpura", "Prahlad Nagar", "Vastrapur", "Maninagar", "Bopal", "Satellite", "Chandkheda", "Gota")),
                IndianCityEntry("Surat", "Gujarat", "GJ", false, "395", "395001", 21.1702, 72.8311, listOf("Vesu", "Adajan", "Varachha", "Piplod", "Katargam", "Pal", "Athwa Lines")),
                IndianCityEntry("Vadodara", "Gujarat", "GJ", false, "390", "390001", 22.3072, 73.1812, listOf("Alkapuri", "Gotri", "Manjalpur", "Fatehgunj", "Waghodia Road", "Karelibaug", "Akota")),
                IndianCityEntry("Rajkot", "Gujarat", "GJ", false, "360", "360001", 22.3039, 70.8022, listOf("Kalawad Road", "150 Feet Ring Road", "Yagnik Road", "Mavdi", "Raiya Road")),
                IndianCityEntry("Gandhinagar", "Gujarat", "GJ", false, "382", "382010", 23.2156, 72.6369, listOf("GIFT City", "Infocity", "Kudasan", "Sargasan", "Sector 21")),
                IndianCityEntry("Bhavnagar", "Gujarat", "GJ", false, "364", "364001", 21.7645, 72.1519, listOf("Waghawadi Road", "Kaliabid", "Chitra", "Ghogha Circle")),
                IndianCityEntry("Jamnagar", "Gujarat", "GJ", false, "361", "361001", 22.4707, 70.0577, listOf("Patel Colony", "Park Colony", "Indira Marg", "Digjam Circle"))
            )
        ),
        IndianStateEntry(
            name = "Haryana",
            code = "HR",
            cities = listOf(
                IndianCityEntry("Gurugram", "Haryana", "HR", false, "122", "122001", 28.4595, 77.0266, listOf("DLF Cyber City", "Golf Course Road", "Sohna Road", "Sector 56", "MG Road", "Palam Vihar", "Udyog Vihar")),
                IndianCityEntry("Faridabad", "Haryana", "HR", false, "121", "121001", 28.4089, 77.3178, listOf("NIT Faridabad", "Sector 15", "Neharpar", "Ballabhgarh", "Surajkund")),
                IndianCityEntry("Panipat", "Haryana", "HR", false, "132", "132103", 29.3909, 76.9635, listOf("Model Town", "GT Road", "Sector 13-17", "Samalkha")),
                IndianCityEntry("Ambala", "Haryana", "HR", false, "134", "134003", 30.3782, 76.7767, listOf("Ambala Cantt", "Ambala City", "Model Town", "Baldev Nagar")),
                IndianCityEntry("Karnal", "Haryana", "HR", false, "132", "132001", 29.6857, 76.9905, listOf("Model Town", "Sector 12", "Sector 8", "Kunjpura Road")),
                IndianCityEntry("Hisar", "Haryana", "HR", false, "125", "125001", 29.1492, 75.7217, listOf("Urban Estate", "Model Town", "Red Square Market", "PLA")),
                IndianCityEntry("Rohtak", "Haryana", "HR", false, "124", "124001", 28.8955, 76.6066, listOf("Model Town", "Sector 14", "Delhi Bypass", "DLF Colony"))
            )
        ),
        IndianStateEntry(
            name = "Himachal Pradesh",
            code = "HP",
            cities = listOf(
                IndianCityEntry("Shimla", "Himachal Pradesh", "HP", false, "171", "171001", 31.1048, 77.1734, listOf("Mall Road", "Sanjauli", "New Shimla", "Khalini", "Chotta Shimla", "Kasumpti")),
                IndianCityEntry("Dharamshala", "Himachal Pradesh", "HP", false, "176", "176215", 32.2190, 76.3234, listOf("McLeod Ganj", "Kotwali Bazar", "Dari", "Sidhbari", "Kangra")),
                IndianCityEntry("Manali", "Himachal Pradesh", "HP", false, "175", "175131", 32.2432, 77.1892, listOf("Mall Road", "Old Manali", "Aleo", "Prini", "Kullu")),
                IndianCityEntry("Solan", "Himachal Pradesh", "HP", false, "173", "173212", 30.9045, 77.0967, listOf("The Mall", "Chambaghat", "Saproon", "Baddi"))
            )
        ),
        IndianStateEntry(
            name = "Jharkhand",
            code = "JH",
            cities = listOf(
                IndianCityEntry("Ranchi", "Jharkhand", "JH", false, "834", "834001", 23.3441, 85.3096, listOf("Lalpur", "Kanke Road", "Harmu", "Doranda", "Morabadi", "Bariatu", "Main Road")),
                IndianCityEntry("Jamshedpur", "Jharkhand", "JH", false, "831", "831001", 22.8046, 86.2029, listOf("Bistupur", "Sakchi", "Mango", "Sonari", "Kadma", "Telco", "Adityapur")),
                IndianCityEntry("Dhanbad", "Jharkhand", "JH", false, "826", "826001", 23.7957, 86.4304, listOf("Bank More", "Hirapur", "Saraidhela", "Bartand", "Jharia")),
                IndianCityEntry("Bokaro Steel City", "Jharkhand", "JH", false, "827", "827001", 23.6693, 86.1511, listOf("City Centre Sector 4", "Chas", "Sector 9", "Co-operative Colony")),
                IndianCityEntry("Deoghar", "Jharkhand", "JH", false, "814", "814112", 24.4852, 86.6948, listOf("Tower Chowk", "Castairs Town", "Williams Town", "Jasidih"))
            )
        ),
        IndianStateEntry(
            name = "Karnataka",
            code = "KA",
            cities = listOf(
                IndianCityEntry("Bengaluru", "Karnataka", "KA", false, "560", "560001", 12.9716, 77.5946, listOf("Indiranagar", "Koramangala", "Whitefield", "HSR Layout", "Jayanagar", "Malleshwaram", "Electronic City", "Marathahalli", "JP Nagar", "Yelahanka", "Hebbal", "BTM Layout")),
                IndianCityEntry("Mysuru", "Karnataka", "KA", false, "570", "570001", 12.2958, 76.6394, listOf("Vijayanagar", "Kuvempunagar", "Gokulam", "Saraswathipuram", "Hebbal", "Jayalakshmipuram")),
                IndianCityEntry("Mangaluru", "Karnataka", "KA", false, "575", "575001", 12.9141, 74.8560, listOf("Kadri", "Bejai", "Hampankatta", "Surathkal", "Kankanady", "Bendoorwell")),
                IndianCityEntry("Hubballi-Dharwad", "Karnataka", "KA", false, "580", "580020", 15.3647, 75.1240, listOf("Vidyanagar", "Keshwapur", "Gokul Road", "Deshpande Nagar", "Saptapur")),
                IndianCityEntry("Belagavi", "Karnataka", "KA", false, "590", "590001", 15.8497, 74.4977, listOf("Tilakwadi", "Shahapur", "Sadashiv Nagar", "Camp", "Udyambag")),
                IndianCityEntry("Kalaburagi", "Karnataka", "KA", false, "585", "585101", 17.3297, 76.8343, listOf("Super Market", "Sedam Road", "Jewargi Colony", "Asian Mall Road")),
                IndianCityEntry("Udupi", "Karnataka", "KA", false, "576", "576101", 13.3409, 74.7421, listOf("Manipal", "Kadiyali", "Brahmagiri", "Santhekatte", "Malpe"))
            )
        ),
        IndianStateEntry(
            name = "Kerala",
            code = "KL",
            cities = listOf(
                IndianCityEntry("Kochi", "Kerala", "KL", false, "682", "682001", 9.9312, 76.2673, listOf("Kakkanad", "Edappally", "MG Road", "Palarivattom", "Kaloor", "Panampilly Nagar", "Aluva", "Fort Kochi")),
                IndianCityEntry("Thiruvananthapuram", "Kerala", "KL", false, "695", "695001", 8.5241, 76.9366, listOf("Kazhakkoottam", "Technopark", "Pattom", "Kowdiar", "Vazhuthacaud", "Sreekaryam", "Thampanoor")),
                IndianCityEntry("Kozhikode", "Kerala", "KL", false, "673", "673001", 11.2588, 75.7804, listOf("Mavoor Road", "Nadakkavu", "Chevayur", "Palayam", "Thondayad")),
                IndianCityEntry("Thrissur", "Kerala", "KL", false, "680", "680001", 10.5276, 76.2144, listOf("Swaraj Round", "Punkunnam", "Ollur", "Ayyanthole", "East Fort")),
                IndianCityEntry("Kollam", "Kerala", "KL", false, "691", "691001", 8.8932, 76.6141, listOf("Chinnakada", "Kadappakada", "Kottiyam", "Karunagappally")),
                IndianCityEntry("Kannur", "Kerala", "KL", false, "670", "670001", 11.8745, 75.3704, listOf("Talap", "Caltex", "Thavakkara", "Payyambalam"))
            )
        ),
        IndianStateEntry(
            name = "Madhya Pradesh",
            code = "MP",
            cities = listOf(
                IndianCityEntry("Indore", "Madhya Pradesh", "MP", false, "452", "452001", 22.7196, 75.8577, listOf("Vijay Nagar", "Palasia", "Bhawarkua", "Rau", "Rajwada", "Sudama Nagar", "Super Corridor")),
                IndianCityEntry("Bhopal", "Madhya Pradesh", "MP", false, "462", "462001", 23.2599, 77.4126, listOf("MP Nagar", "Arera Colony", "Kolar Road", "Hoshangabad Road", "New Market", "Bairagarh", "Indrapuri")),
                IndianCityEntry("Gwalior", "Madhya Pradesh", "MP", false, "474", "474001", 26.2183, 78.1828, listOf("City Centre", "Lashkar", "Morar", "Thatipur", "DD Nagar")),
                IndianCityEntry("Jabalpur", "Madhya Pradesh", "MP", false, "482", "482001", 23.1815, 79.9864, listOf("Napier Town", "Wright Town", "Vijay Nagar", "Civil Lines", "Adhartal")),
                IndianCityEntry("Ujjain", "Madhya Pradesh", "MP", false, "456", "456001", 23.1765, 75.7885, listOf("Freeganj", "Nanakheda", "Mahakal Road", "Indira Nagar"))
            )
        ),
        IndianStateEntry(
            name = "Maharashtra",
            code = "MH",
            cities = listOf(
                IndianCityEntry("Mumbai", "Maharashtra", "MH", false, "400", "400001", 19.0760, 72.8777, listOf("Andheri West", "Andheri East", "Bandra", "Powai", "Thane", "Dadar", "Navi Mumbai", "Borivali", "Malad", "Goregaon", "Lower Parel", "Kurla", "Vashi")),
                IndianCityEntry("Pune", "Maharashtra", "MH", false, "411", "411001", 18.5204, 73.8567, listOf("Kothrud", "Koregaon Park", "Viman Nagar", "Baner", "Hinjewadi", "Wakad", "Kharadi", "Hadapsar", "Pimpri-Chinchwad", "Aundh", "Magarpatta")),
                IndianCityEntry("Nagpur", "Maharashtra", "MH", false, "440", "440001", 21.1458, 79.0882, listOf("Dharampeth", "Sitabuldi", "Sadar", "Manish Nagar", "Wardha Road", "Pratap Nagar", "Trimurti Nagar")),
                IndianCityEntry("Nashik", "Maharashtra", "MH", false, "422", "422001", 19.9975, 73.7898, listOf("College Road", "Gangapur Road", "Indira Nagar", "Cidco", "Panchavati", "Nashik Road")),
                IndianCityEntry("Chhatrapati Sambhajinagar (Aurangabad)", "Maharashtra", "MH", false, "431", "431001", 19.8762, 75.3433, listOf("CIDCO", "Garkheda", "Nirala Bazar", "Waluj", "Usmanpura")),
                IndianCityEntry("Kolhapur", "Maharashtra", "MH", false, "416", "416001", 16.7050, 74.2433, listOf("Rajarampuri", "Shahupuri", "Tarabai Park", "Rankala", "Laxmipuri")),
                IndianCityEntry("Solapur", "Maharashtra", "MH", false, "413", "413001", 17.6599, 75.9064, listOf("Saat Rasta", "Jule Solapur", "Vijapur Road", "Murarji Peth"))
            )
        ),
        IndianStateEntry(
            name = "Manipur",
            code = "MN",
            cities = listOf(
                IndianCityEntry("Imphal", "Manipur", "MN", false, "795", "795001", 24.8170, 93.9368, listOf("Thangal Bazar", "Paona Bazar", "Lamphelpat", "Singjamei", "Porompat"))
            )
        ),
        IndianStateEntry(
            name = "Meghalaya",
            code = "ML",
            cities = listOf(
                IndianCityEntry("Shillong", "Meghalaya", "ML", false, "793", "793001", 25.5788, 91.8933, listOf("Police Bazar", "Laitumkhrah", "Bishnupur", "Mawlai", "Nongthymmai")),
                IndianCityEntry("Tura", "Meghalaya", "ML", false, "794", "794001", 25.5141, 90.2032, listOf("Araimile", "Hawakhana", "Rongkhon", "Tura Bazar"))
            )
        ),
        IndianStateEntry(
            name = "Mizoram",
            code = "MZ",
            cities = listOf(
                IndianCityEntry("Aizawl", "Mizoram", "MZ", false, "796", "796001", 23.7271, 92.7176, listOf("Dawrpui", "Zarkawt", "Chanmari", "Bawngkawn", "Chaltlang"))
            )
        ),
        IndianStateEntry(
            name = "Nagaland",
            code = "NL",
            cities = listOf(
                IndianCityEntry("Kohima", "Nagaland", "NL", false, "797", "797001", 25.6751, 94.1086, listOf("PR Hill", "High School Junction", "Main Town", "Chandmari")),
                IndianCityEntry("Dimapur", "Nagaland", "NL", false, "797", "797112", 25.9091, 93.7266, listOf("Circular Road", "Chumukedima", "Purana Bazar", "Nagarjan"))
            )
        ),
        IndianStateEntry(
            name = "Odisha",
            code = "OD",
            cities = listOf(
                IndianCityEntry("Bhubaneswar", "Odisha", "OD", false, "751", "751001", 20.2961, 85.8245, listOf("Patia", "Saheed Nagar", "Jayadev Vihar", "Khandagiri", "Nayapalli", "Chandrasekharpur", "Rasulgarh")),
                IndianCityEntry("Cuttack", "Odisha", "OD", false, "753", "753001", 20.4625, 85.8830, listOf("CDA Sector 6", "Badambadi", "College Square", "Buxi Bazar", "Mahanadi Vihar")),
                IndianCityEntry("Rourkela", "Odisha", "OD", false, "769", "769001", 22.2604, 84.8536, listOf("Chhend Colony", "Civil Township", "Udit Nagar", "Koel Nagar")),
                IndianCityEntry("Berhampur", "Odisha", "OD", false, "760", "760001", 19.3150, 84.7941, listOf("Gandhi Nagar", "Bhabha Nagar", "Kamapalli", "Ambapua")),
                IndianCityEntry("Sambalpur", "Odisha", "OD", false, "768", "768001", 21.4669, 83.9812, listOf("Ainthapali", "Burla", "Budharaja", "Dhanupali")),
                IndianCityEntry("Puri", "Odisha", "OD", false, "752", "752001", 19.8135, 85.8312, listOf("Grand Road", "Swargadwar", "Chakra Tirtha Road", "Talabania"))
            )
        ),
        IndianStateEntry(
            name = "Punjab",
            code = "PB",
            cities = listOf(
                IndianCityEntry("Ludhiana", "Punjab", "PB", false, "141", "141001", 30.9010, 75.8573, listOf("Sarabha Nagar", "Model Town", "Civil Lines", "Ferozepur Road", "Dugri", "BRS Nagar")),
                IndianCityEntry("Amritsar", "Punjab", "PB", false, "143", "143001", 31.6340, 74.8723, listOf("Ranjit Avenue", "Lawrence Road", "Mall Road", "Majitha Road", "Chheharta")),
                IndianCityEntry("Jalandhar", "Punjab", "PB", false, "144", "144001", 31.3260, 75.5762, listOf("Model Town", "Urban Estate", "Rama Mandi", "Maqsudan", "Nakodar Road")),
                IndianCityEntry("Mohali", "Punjab", "PB", false, "160", "160055", 30.7046, 76.7179, listOf("Phase 7", "Sector 70", "Kharar", "Zirakpur", "Aerocity", "IT City")),
                IndianCityEntry("Patiala", "Punjab", "PB", false, "147", "147001", 30.3398, 76.3869, listOf("Leela Bhawan", "Urban Estate", "Model Town", "Tripuri")),
                IndianCityEntry("Bathinda", "Punjab", "PB", false, "151", "151001", 30.2110, 74.9455, listOf("Model Town", "Civil Lines", "Goniana Road", "Power House Road"))
            )
        ),
        IndianStateEntry(
            name = "Rajasthan",
            code = "RJ",
            cities = listOf(
                IndianCityEntry("Jaipur", "Rajasthan", "RJ", false, "302", "302001", 26.9124, 75.7873, listOf("Malviya Nagar", "Vaishali Nagar", "C-Scheme", "Mansarovar", "Jagatpura", "Tonk Road", "Raja Park", "Jhotwara")),
                IndianCityEntry("Jodhpur", "Rajasthan", "RJ", false, "342", "342001", 26.2389, 73.0243, listOf("Sardarpura", "Ratanada", "Shastri Nagar", "Chopasni Housing Board", "Paota")),
                IndianCityEntry("Udaipur", "Rajasthan", "RJ", false, "313", "313001", 24.5854, 73.7125, listOf("Hiran Magri", "Fatehpura", "Bhuwana", "Sukhadia Circle", "Panchwati")),
                IndianCityEntry("Kota", "Rajasthan", "RJ", false, "324", "324001", 25.2138, 75.8648, listOf("Talwandi", "Mahaveer Nagar", "Vigyan Nagar", "Rajeev Gandhi Nagar", "Gumanpura")),
                IndianCityEntry("Ajmer", "Rajasthan", "RJ", false, "305", "305001", 26.4499, 74.6399, listOf("Vaishali Nagar", "Civil Lines", "Kishangarh", "Pushkar Road")),
                IndianCityEntry("Bikaner", "Rajasthan", "RJ", false, "334", "334001", 28.0229, 73.3119, listOf("JNV Colony", "Rani Bazar", "Sadul Ganj", "Gangashahar"))
            )
        ),
        IndianStateEntry(
            name = "Sikkim",
            code = "SK",
            cities = listOf(
                IndianCityEntry("Gangtok", "Sikkim", "SK", false, "737", "737101", 27.3389, 88.6065, listOf("MG Marg", "Tadong", "Deorali", "Ranipool", "Development Area"))
            )
        ),
        IndianStateEntry(
            name = "Tamil Nadu",
            code = "TN",
            cities = listOf(
                IndianCityEntry("Chennai", "Tamil Nadu", "TN", false, "600", "600001", 13.0827, 80.2707, listOf("Adyar", "T. Nagar", "Anna Nagar", "Velachery", "OMR", "Tambaram", "Porur", "Guindy", "Mylapore", "Nungambakkam", "Sholinganallur")),
                IndianCityEntry("Coimbatore", "Tamil Nadu", "TN", false, "641", "641001", 11.0168, 76.9558, listOf("RS Puram", "Gandhipuram", "Peelamedu", "Saravanampatti", "Saibaba Colony", "Singanallur")),
                IndianCityEntry("Madurai", "Tamil Nadu", "TN", false, "625", "625001", 9.9252, 78.1198, listOf("Anna Nagar", "KK Nagar", "SS Colony", "Mattuthavani", "Thirunagar")),
                IndianCityEntry("Tiruchirappalli", "Tamil Nadu", "TN", false, "620", "620001", 10.7905, 78.7047, listOf("Thillai Nagar", "Srirangam", "KK Nagar", "Cantonment", "Woraiyur")),
                IndianCityEntry("Salem", "Tamil Nadu", "TN", false, "636", "636001", 11.6643, 78.1460, listOf("Fairlands", "Alagapuram", "Suramangalam", "Hasthampatti")),
                IndianCityEntry("Tiruppur", "Tamil Nadu", "TN", false, "641", "641601", 11.1085, 77.3411, listOf("Avinashi Road", "PN Road", "Palladam Road", "Kangeyam Road")),
                IndianCityEntry("Vellore", "Tamil Nadu", "TN", false, "632", "632001", 12.9165, 79.1325, listOf("Katpadi", "Gandhi Nagar", "Sathuvachari", "Bagayam"))
            )
        ),
        IndianStateEntry(
            name = "Telangana",
            code = "TG",
            cities = listOf(
                IndianCityEntry("Hyderabad", "Telangana", "TG", false, "500", "500001", 17.3850, 78.4867, listOf("Madhapur", "Gachibowli", "Banjara Hills", "Jubilee Hills", "Kukatpally", "Hitech City", "Kondapur", "Secunderabad", "Begumpet", "Miyapur", "LB Nagar", "Ameerpet")),
                IndianCityEntry("Warangal", "Telangana", "TG", false, "506", "506001", 17.9689, 79.5941, listOf("Hanamkonda", "Kazipet", "Subedari", "Nakkalagutta")),
                IndianCityEntry("Nizamabad", "Telangana", "TG", false, "503", "503001", 18.6725, 78.0941, listOf("Vinayak Nagar", "Khaleelwadi", "Subhash Nagar", "Armoor Road")),
                IndianCityEntry("Karimnagar", "Telangana", "TG", false, "505", "505001", 18.4386, 79.1288, listOf("Mankamma Thota", "Jyothinagar", "Bhagatnagar", "Kothirampur"))
            )
        ),
        IndianStateEntry(
            name = "Tripura",
            code = "TR",
            cities = listOf(
                IndianCityEntry("Agartala", "Tripura", "TR", false, "799", "799001", 23.8315, 91.2868, listOf("Banamalipur", "Krishnanagar", "Ramnagar", "Indranagar", "Battala"))
            )
        ),
        IndianStateEntry(
            name = "Uttar Pradesh",
            code = "UP",
            cities = listOf(
                IndianCityEntry("Lucknow", "Uttar Pradesh", "UP", false, "226", "226001", 26.8467, 80.9462, listOf("Gomti Nagar", "Hazratganj", "Aliganj", "Indira Nagar", "Mahanagar", "Alambagh", "Jankipuram", "Sushant Golf City")),
                IndianCityEntry("Noida", "Uttar Pradesh", "UP", false, "201", "201301", 28.5355, 77.3910, listOf("Sector 62", "Sector 18", "Sector 137", "Greater Noida West", "Sector 15", "Sector 50", "Knowledge Park")),
                IndianCityEntry("Ghaziabad", "Uttar Pradesh", "UP", false, "201", "201001", 28.6692, 77.4538, listOf("Indirapuram", "Vaishali", "Raj Nagar Extension", "Vasundhara", "Kaushambi", "Crossings Republik")),
                IndianCityEntry("Kanpur", "Uttar Pradesh", "UP", false, "208", "208001", 26.4499, 80.3319, listOf("Swaroop Nagar", "Kakadeo", "Civil Lines", "Kalyanpur", "Govind Nagar", "Kidwai Nagar")),
                IndianCityEntry("Varanasi", "Uttar Pradesh", "UP", false, "221", "221001", 25.3176, 82.9739, listOf("Lanka", "Sigra", "Bhelupur", "Mahmoorganj", "Cantonment", "Sarnath")),
                IndianCityEntry("Prayagraj", "Uttar Pradesh", "UP", false, "211", "211001", 25.4358, 81.8463, listOf("Civil Lines", "Katra", "Naini", "Jhunsi", "George Town", "Tagore Town")),
                IndianCityEntry("Agra", "Uttar Pradesh", "UP", false, "282", "282001", 27.1767, 78.0081, listOf("Sanjay Place", "Kamla Nagar", "Sikandra", "Dayalbagh", "Tajganj")),
                IndianCityEntry("Meerut", "Uttar Pradesh", "UP", false, "250", "250001", 28.9845, 77.7064, listOf("Shastri Nagar", "Ganga Nagar", "Saket", "Modipuram", "Kanker Khera")),
                IndianCityEntry("Gorakhpur", "Uttar Pradesh", "UP", false, "273", "273001", 26.7606, 83.3732, listOf("Golghar", "Taramandal", "Betiahata", "Rapti Nagar", "Mohaddipur")),
                IndianCityEntry("Bareilly", "Uttar Pradesh", "UP", false, "243", "243001", 28.3670, 79.4304, listOf("Civil Lines", "Rajendra Nagar", "DD Puram", "Izzatnagar"))
            )
        ),
        IndianStateEntry(
            name = "Uttarakhand",
            code = "UK",
            cities = listOf(
                IndianCityEntry("Dehradun", "Uttarakhand", "UK", false, "248", "248001", 30.3165, 78.0322, listOf("Rajpur Road", "Vasant Vihar", "Sahastradhara Road", "Clement Town", "Prem Nagar", "Ballupur")),
                IndianCityEntry("Haridwar", "Uttarakhand", "UK", false, "249", "249401", 29.9457, 78.1642, listOf("Ranipur More", "SIDCUL", "Jwalapur", "Kankhal", "Shivalik Nagar")),
                IndianCityEntry("Rishikesh", "Uttarakhand", "UK", false, "249", "249201", 30.0869, 78.2676, listOf("Tapovan", "Laxman Jhula", "AIIMS Road", "Shyampur")),
                IndianCityEntry("Haldwani", "Uttarakhand", "UK", false, "263", "263139", 29.2183, 79.5130, listOf("Kusumkhera", "Mukhani", "Kathgodam", "Nainital Road"))
            )
        ),
        IndianStateEntry(
            name = "West Bengal",
            code = "WB",
            cities = listOf(
                IndianCityEntry("Kolkata", "West Bengal", "WB", false, "700", "700001", 22.5726, 88.3639, listOf("Salt Lake", "Park Street", "New Town", "Ballygunge", "Howrah", "Dum Dum", "Gariahat", "Jadavpur", "Behala", "Tollygunge", "Rajarhat")),
                IndianCityEntry("Siliguri", "West Bengal", "WB", false, "734", "734001", 26.7271, 88.3953, listOf("Sevoke Road", "Pradhan Nagar", "Matigara", "Hakimpara", "Ashrampara")),
                IndianCityEntry("Durgapur", "West Bengal", "WB", false, "713", "713201", 23.5204, 87.3119, listOf("City Centre", "Bidhannagar", "Benachity", "Muchipara")),
                IndianCityEntry("Asansol", "West Bengal", "WB", false, "713", "713301", 23.6739, 86.9524, listOf("Burnpur Road", "Kalyanpur", "Sen Raleigh Road", "Ushagram")),
                IndianCityEntry("Kharagpur", "West Bengal", "WB", false, "721", "721301", 22.3460, 87.2320, listOf("Inda", "Malancha", "Prembazar", "Nimpura"))
            )
        ),

        // ==================== 8 UNION TERRITORIES ====================
        IndianStateEntry(
            name = "Delhi NCR",
            code = "DL",
            isUnionTerritory = true,
            cities = listOf(
                IndianCityEntry("Delhi NCR", "Delhi NCR", "DL", true, "110", "110001", 28.6139, 77.2090, listOf("Connaught Place", "South Extension", "Dwarka", "Gurugram Cyber City", "Noida Sector 62", "Indirapuram", "Rohini", "Saket", "Lajpat Nagar", "Karol Bagh", "Janakpuri", "Vasant Kunj", "Pitampura", "Mayur Vihar", "Hauz Khas"))
            )
        ),
        IndianStateEntry(
            name = "Chandigarh",
            code = "CH",
            isUnionTerritory = true,
            cities = listOf(
                IndianCityEntry("Chandigarh", "Chandigarh", "CH", true, "160", "160017", 30.7333, 76.7794, listOf("Sector 17", "Sector 35", "Sector 22", "Manimajra", "IT Park", "Elante Industrial Area", "Panchkula"))
            )
        ),
        IndianStateEntry(
            name = "Jammu and Kashmir",
            code = "JK",
            isUnionTerritory = true,
            cities = listOf(
                IndianCityEntry("Srinagar", "Jammu and Kashmir", "JK", true, "190", "190001", 34.0837, 74.7973, listOf("Lal Chowk", "Rajbagh", "Karan Nagar", "Bemina", "Hyderpora", "Hazratbal")),
                IndianCityEntry("Jammu", "Jammu and Kashmir", "JK", true, "180", "180001", 32.7266, 74.8570, listOf("Gandhi Nagar", "Trikuta Nagar", "Channi Himmat", "Janipur", "Talab Tillo"))
            )
        ),
        IndianStateEntry(
            name = "Puducherry",
            code = "PY",
            isUnionTerritory = true,
            cities = listOf(
                IndianCityEntry("Puducherry", "Puducherry", "PY", true, "605", "605001", 11.9416, 79.8083, listOf("White Town", "Lawspet", "Muthialpet", "Reddiarpalayam", "Mudaliarpet", "Villianur"))
            )
        ),
        IndianStateEntry(
            name = "Ladakh",
            code = "LA",
            isUnionTerritory = true,
            cities = listOf(
                IndianCityEntry("Leh", "Ladakh", "LA", true, "194", "194101", 34.1526, 77.5771, listOf("Main Bazar", "Changspa", "Choglamsar", "Skara", "Kargil"))
            )
        ),
        IndianStateEntry(
            name = "Andaman and Nicobar Islands",
            code = "AN",
            isUnionTerritory = true,
            cities = listOf(
                IndianCityEntry("Port Blair (Sri Vijaya Puram)", "Andaman and Nicobar Islands", "AN", true, "744", "744101", 11.6234, 92.7265, listOf("Aberdeen Bazar", "Junglighat", "Garacharma", "Haddo", "Bathu Basti"))
            )
        ),
        IndianStateEntry(
            name = "Dadra and Nagar Haveli and Daman and Diu",
            code = "DD",
            isUnionTerritory = true,
            cities = listOf(
                IndianCityEntry("Silvassa", "Dadra and Nagar Haveli and Daman and Diu", "DD", true, "396", "396230", 20.2766, 73.0083, listOf("Amli", "Samarvarni", "Naroli Road", "Piparia")),
                IndianCityEntry("Daman", "Dadra and Nagar Haveli and Daman and Diu", "DD", true, "396", "396210", 20.3974, 72.8328, listOf("Nani Daman", "Moti Daman", "Devka", "Dabhel"))
            )
        ),
        IndianStateEntry(
            name = "Lakshadweep",
            code = "LD",
            isUnionTerritory = true,
            cities = listOf(
                IndianCityEntry("Kavaratti", "Lakshadweep", "LD", true, "682", "682555", 10.5667, 72.6417, listOf("Kavaratti Island", "Agatti", "Minicoy", "Andrott"))
            )
        )
    )

    val allCities: List<IndianCityEntry> by lazy {
        // Put top tier-1 hubs first for quick access, followed by all other cities alphabetically
        val priorityOrder = listOf(
            "Bengaluru", "Mumbai", "Delhi NCR", "Hyderabad", "Pune",
            "Chennai", "Kolkata", "Ahmedabad", "Jaipur", "Lucknow",
            "Gurugram", "Noida", "Kochi", "Chandigarh", "Indore",
            "Bhopal", "Patna", "Surat", "Coimbatore", "Visakhapatnam",
            "Nagpur", "Bhubaneswar", "Guwahati", "Ranchi", "Dehradun"
        )
        val raw = allStatesAndUTs.flatMap { it.cities }
        val prioritySet = priorityOrder.toSet()
        val prioritized = priorityOrder.mapNotNull { name -> raw.find { it.name == name } }
        val rest = raw.filter { it.name !in prioritySet }.sortedBy { it.name }
        prioritized + rest
    }

    /**
     * Backward-compatible map of all Indian cities to their localities,
     * starting with "All India" -> ["All Localities"].
     */
    val indianCitiesMap: Map<String, List<String>> by lazy {
        val map = LinkedHashMap<String, List<String>>()
        map["All India"] = listOf("All Localities")
        allCities.forEach { city ->
            map[city.name] = city.localities
        }
        map
    }

    fun findCityEntry(cityName: String): IndianCityEntry? {
        val clean = cityName.trim()
        if (clean.isEmpty() || clean.equals("All India", ignoreCase = true)) return null
        return allCities.firstOrNull {
            it.name.equals(clean, ignoreCase = true) ||
                clean.contains(it.name, ignoreCase = true) ||
                it.name.contains(clean, ignoreCase = true)
        }
    }

    fun stateForCity(cityName: String): String {
        return findCityEntry(cityName)?.state ?: "Karnataka"
    }

    fun defaultPinForCity(cityName: String): String {
        return findCityEntry(cityName)?.defaultPin ?: ""
    }

    fun localitiesForCity(cityName: String): List<String> {
        if (cityName.equals("All India", ignoreCase = true)) return listOf("All Localities")
        return findCityEntry(cityName)?.localities ?: emptyList()
    }

    /**
     * Searches across all 28 States + 8 UTs by State name, City/District name, Locality, or PIN code prefix.
     */
    fun searchCities(
        query: String,
        selectedState: String = "All States & UTs"
    ): List<IndianCityEntry> {
        val q = query.trim().lowercase()
        val stateScoped = if (selectedState.isBlank() || selectedState == "All States & UTs") {
            allCities
        } else {
            allCities.filter { it.state.equals(selectedState, ignoreCase = true) }
        }
        if (q.isEmpty()) return stateScoped
        return stateScoped.filter { entry ->
            entry.name.lowercase().contains(q) ||
                entry.state.lowercase().contains(q) ||
                entry.stateCode.lowercase() == q ||
                entry.defaultPin.startsWith(q) ||
                entry.pinPrefix.startsWith(q.take(3)) ||
                entry.localities.any { loc -> loc.lowercase().contains(q) }
        }
    }

    /**
     * Resolves any 6-digit Indian PIN code using the live India Post Postal PIN API
     * (`https://api.postalpincode.in/pincode/{pin}`) with an instant offline All-India fallback.
     */
    suspend fun resolvePinCode(pinCode: String): Result<ResolvedIndiaLocation> = withContext(Dispatchers.IO) {
        val cleanPin = pinCode.trim().filter { it.isDigit() }
        if (cleanPin.length != 6 || cleanPin.startsWith("0")) {
            return@withContext Result.failure(IllegalArgumentException("Enter a valid 6-digit Indian PIN code (e.g., 560038, 110001, 400050)."))
        }

        // 1. Try live India Post API first (with short timeout so UI stays snappy)
        try {
            val url = URL("https://api.postalpincode.in/pincode/$cleanPin")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val rootArray = JSONArray(responseText)
                if (rootArray.length() > 0) {
                    val firstObj = rootArray.getJSONObject(0)
                    val status = firstObj.optString("Status")
                    val postOfficeArray = firstObj.optJSONArray("PostOffice")
                    if (status.equals("Success", ignoreCase = true) && postOfficeArray != null && postOfficeArray.length() > 0) {
                        val postOffices = mutableListOf<String>()
                        var district = ""
                        var state = ""
                        for (i in 0 until postOfficeArray.length()) {
                            val po = postOfficeArray.getJSONObject(i)
                            val poName = po.optString("Name").trim()
                            if (poName.isNotBlank()) postOffices.add(poName)
                            if (district.isBlank()) district = po.optString("District").trim()
                            if (state.isBlank()) state = po.optString("State").trim()
                        }
                        val matchedDirectoryCity = findCityEntry(district)
                            ?: allCities.firstOrNull { it.defaultPin == cleanPin || cleanPin.startsWith(it.pinPrefix) }
                        val resolvedCity = matchedDirectoryCity?.name ?: district.ifBlank { "Bengaluru" }
                        val resolvedState = state.ifBlank { matchedDirectoryCity?.state ?: "India" }
                        val resolvedLocality = postOffices.firstOrNull().orEmpty()
                        return@withContext Result.success(
                            ResolvedIndiaLocation(
                                state = resolvedState,
                                city = resolvedCity,
                                locality = resolvedLocality,
                                pinCode = cleanPin,
                                postOffices = postOffices.distinct(),
                                source = "India Post PIN Directory"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Fall through to offline PIN prefix index
        }

        // 2. Offline All-India PIN code & Postal Circle prefix resolution
        val exactOrPrefixCity = allCities.firstOrNull { it.defaultPin == cleanPin }
            ?: allCities.firstOrNull { cleanPin.startsWith(it.pinPrefix) }
            ?: resolveByPostalZoneDigit(cleanPin)

        if (exactOrPrefixCity != null) {
            val locality = exactOrPrefixCity.localities.firstOrNull().orEmpty()
            Result.success(
                ResolvedIndiaLocation(
                    state = exactOrPrefixCity.state,
                    city = exactOrPrefixCity.name,
                    locality = locality,
                    pinCode = cleanPin,
                    postOffices = exactOrPrefixCity.localities,
                    source = "Offline All-India PIN Index"
                )
            )
        } else {
            Result.failure(IllegalArgumentException("Could not resolve PIN code $cleanPin."))
        }
    }

    private fun resolveByPostalZoneDigit(cleanPin: String): IndianCityEntry? {
        val prefix2 = cleanPin.take(2)
        val targetCityName = when (prefix2) {
            "11" -> "Delhi NCR"
            "12", "13" -> "Gurugram"
            "14", "15" -> "Ludhiana"
            "16" -> "Chandigarh"
            "17" -> "Shimla"
            "18", "19" -> "Jammu"
            "20", "21", "22", "23", "24", "25", "26", "27", "28" -> if (prefix2 == "20") "Noida" else "Lucknow"
            "30", "31", "32", "33", "34" -> "Jaipur"
            "36", "37", "38", "39" -> if (prefix2 == "39") "Surat" else "Ahmedabad"
            "40" -> if (cleanPin.startsWith("403")) "Panaji" else "Mumbai"
            "41" -> "Pune"
            "42", "43", "44" -> if (prefix2 == "44") "Nagpur" else "Nashik"
            "45", "46", "47", "48" -> if (prefix2 == "45") "Indore" else "Bhopal"
            "49" -> "Raipur"
            "50" -> "Hyderabad"
            "51", "52", "53" -> "Visakhapatnam"
            "56", "57", "58", "59" -> "Bengaluru"
            "60", "61", "62", "63", "64" -> if (cleanPin.startsWith("605")) "Puducherry" else "Chennai"
            "67", "68", "69" -> "Kochi"
            "70", "71", "72", "73", "74" -> "Kolkata"
            "75", "76", "77" -> "Bhubaneswar"
            "78" -> "Guwahati"
            "79" -> "Shillong"
            "80", "81", "82", "84", "85" -> "Patna"
            "83" -> "Ranchi"
            else -> "Bengaluru"
        }
        return findCityEntry(targetCityName)
    }

    /**
     * Detects the user's real Indian location via Android LocationManager + Geocoder reverse-geocoding.
     */
    @SuppressLint("MissingPermission")
    suspend fun detectCurrentIndiaLocation(context: Context): Result<ResolvedIndiaLocation> = withContext(Dispatchers.IO) {
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasCoarse && !hasFine) {
            return@withContext Result.failure(SecurityException("Location permission is not granted."))
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext Result.failure(IllegalStateException("Location service unavailable on this device."))

        val location: Location? = getBestAvailableLocation(locationManager)
        if (location != null) {
            // Check if coordinates are within or near India bounding box (Lat 6°N..38°N, Lon 68°E..98°E)
            val isWithinIndiaBounds = location.latitude in 6.0..38.0 && location.longitude in 68.0..98.0

            if (Geocoder.isPresent()) {
                try {
                    val geocoder = Geocoder(context, Locale("en", "IN"))
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 3)
                    val addr = addresses?.firstOrNull()
                    if (addr != null && (addr.countryCode.equals("IN", ignoreCase = true) || isWithinIndiaBounds)) {
                        val rawCity = addr.locality ?: addr.subAdminArea ?: addr.adminArea.orEmpty()
                        val rawState = addr.adminArea.orEmpty()
                        val rawLocality = addr.subLocality ?: addr.thoroughfare ?: addr.featureName.orEmpty()
                        val rawPin = addr.postalCode?.filter { it.isDigit() }?.take(6).orEmpty()

                        val matchedCity = findCityEntry(rawCity)
                            ?: nearestIndianCity(location.latitude, location.longitude)
                        val finalCity = matchedCity.name.ifBlank { rawCity.ifBlank { "Bengaluru" } }
                        val finalState = rawState.ifBlank { matchedCity.state }
                        val finalPin = rawPin.ifBlank { matchedCity.defaultPin }
                        val finalLocality = rawLocality.takeIf { it.isNotBlank() && !it.equals(finalCity, ignoreCase = true) }
                            ?: matchedCity.localities.firstOrNull().orEmpty()

                        return@withContext Result.success(
                            ResolvedIndiaLocation(
                                state = finalState,
                                city = finalCity,
                                locality = finalLocality,
                                pinCode = finalPin,
                                postOffices = matchedCity.localities,
                                source = "Live GPS + Reverse Geocoder"
                            )
                        )
                    }
                } catch (_: Exception) {
                    // Fall through to coordinate distance matching
                }
            }

            if (isWithinIndiaBounds) {
                val nearest = nearestIndianCity(location.latitude, location.longitude)
                return@withContext Result.success(
                    ResolvedIndiaLocation(
                        state = nearest.state,
                        city = nearest.name,
                        locality = nearest.localities.firstOrNull().orEmpty(),
                        pinCode = nearest.defaultPin,
                        postOffices = nearest.localities,
                        source = "Live GPS Coordinates"
                    )
                )
            }
        }

        // Fallback when running on a cloud emulator whose GPS is either disabled or set to default US coordinates
        val fallback = findCityEntry("Bengaluru") ?: allCities.first()
        Result.success(
            ResolvedIndiaLocation(
                state = fallback.state,
                city = fallback.name,
                locality = fallback.localities.firstOrNull().orEmpty(),
                pinCode = fallback.defaultPin,
                postOffices = fallback.localities,
                source = "Default India Hub (Emulator GPS outside India)"
            )
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun getBestAvailableLocation(locationManager: LocationManager): Location? {
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }

        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val last = locationManager.getLastKnownLocation(provider)
                    if (last != null) {
                        if (bestLocation == null || last.time > bestLocation.time) {
                            bestLocation = last
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        if (bestLocation != null) return bestLocation

        // Request a single fresh location update with a 2.5s timeout on main looper
        val activeProvider = providers.firstOrNull {
            try {
                locationManager.isProviderEnabled(it)
            } catch (_: Exception) {
                false
            }
        } ?: return null

        return withTimeoutOrNull(2500L) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        try {
                            locationManager.removeUpdates(this)
                        } catch (_: Exception) {
                        }
                        if (cont.isActive) cont.resume(location)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                try {
                    @Suppress("DEPRECATION")
                    locationManager.requestSingleUpdate(activeProvider, listener, Looper.getMainLooper())
                    cont.invokeOnCancellation {
                        try {
                            locationManager.removeUpdates(listener)
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    private fun nearestIndianCity(lat: Double, lon: Double): IndianCityEntry {
        return allCities.minByOrNull { city ->
            val dLat = city.latitude - lat
            val dLon = city.longitude - lon
            dLat * dLat + dLon * dLon
        } ?: allCities.first()
    }
}
