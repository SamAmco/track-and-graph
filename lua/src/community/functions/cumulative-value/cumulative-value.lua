-- Lua Function to calculate cumulative sum of data point values
-- This function computes a running total, optionally resetting on specific labels

local checkbox = require("tng.config").checkbox
local text = require("tng.config").text

return {
    -- Configuration metadata
    id = "cumulative-value",
    version = "1.0.1",
    inputCount = 1,
    categories = {"_arithmetic"},
    title = {
    	["en"] = "Cumulative Value",
    	["af"] = "Kumulatiewe Waarde",
    	["sq"] = "Vlerë kumulative",
    	["am"] = "ድምር ዋጋ",
    	["hy"] = "Կուտակային արժեք",
    	["az"] = "Toplanan qiymət",
    	["bn"] = "ক্রমযোজিত মান",
    	["eu"] = "Balio metatua",
    	["be"] = "Назапашвальнае значэнне",
    	["bg"] = "Кумулативна стойност",
    	["my"] = "စုစုပေါင်းတိုးလာသော တန်ဖိုး",
    	["ca"] = "Valor acumulat",
    	["zh-Hans"] = "累计值",
    	["zh-Hant"] = "累計值",
    	["hr"] = "Kumulativna vrijednost",
    	["cs"] = "Kumulativní hodnota",
    	["da"] = "Kumulativ værdi",
    	["nl"] = "Cumulatieve waarde",
    	["et"] = "Kumulatiivne väärtus",
    	["fil"] = "Pinagsama-samang Halaga",
    	["fi"] = "Kumulatiivinen arvo",
    	["fr"] = "Valeur cumulée",
    	["gl"] = "Valor acumulado",
    	["ka"] = "კუმულაციური მნიშვნელობა",
    	["de"] = "Kumulativer Wert",
    	["el"] = "Σωρευτική τιμή",
    	["gu"] = "સંચિત મૂલ્ય",
    	["hi"] = "संचयी मान",
    	["hu"] = "Halmozott érték",
    	["is"] = "Uppsafnað gildi",
    	["id"] = "Nilai Kumulatif",
    	["it"] = "Valore cumulativo",
    	["ja"] = "累積値",
    	["kn"] = "ಸಂಚಿತ ಮೌಲ್ಯ",
    	["kk"] = "Жинақталған мән",
    	["km"] = "តម្លៃបង្គរ",
    	["ko"] = "누적 값",
    	["ky"] = "Жыйынды маани",
    	["lo"] = "ຄ່າສະສົມ",
    	["lv"] = "Kumulatīvā vērtība",
    	["lt"] = "Kaupiamoji reikšmė",
    	["mk"] = "Кумулативна вредност",
    	["ms"] = "Nilai Terkumpul",
    	["ml"] = "സഞ്ചിത മൂല്യം",
    	["mr"] = "संचयी मूल्य",
    	["mn"] = "Хуримтлагдсан утга",
    	["ne"] = "सञ्चित मान",
    	["no"] = "Kumulativ verdi",
    	["pl"] = "Wartość skumulowana",
    	["pt"] = "Valor cumulativo",
    	["pa"] = "ਸੰਚਿਤ ਮੁੱਲ",
    	["ro"] = "Valoare cumulativă",
    	["rm"] = "Valur cumulativa",
    	["ru"] = "Накопленное значение",
    	["sr"] = "Kumulativna vrednost",
    	["si"] = "සමුච්චිත අගය",
    	["sk"] = "Kumulatívna hodnota",
    	["sl"] = "Kumulativna vrednost",
    	["es"] = "Valor acumulado",
    	["sw"] = "Thamani Jumlifu",
    	["sv"] = "Kumulativt värde",
    	["ta"] = "கூட்டுத்தொகை மதிப்பு",
    	["te"] = "సంచిత విలువ",
    	["th"] = "ค่าทบสะสม",
    	["tr"] = "Birikimli Değer",
    	["uk"] = "Накопичувальне значення",
    	["vi"] = "Giá trị tích lũy",
    },
    description = {
    	["en"] = "Calculates the cumulative sum of data point values. Optionally resets accumulation when a specific label is encountered.",
    	["af"] = "Bereken die kumulatiewe som van datapuntwaardes. Stel die akkumulering opsioneel terug wanneer ’n spesifieke etiket gevind word.",
    	["sq"] = "Llogarit shumën kumulative të vlerave të pikave të të dhënave. Opsionalisht, rivendos grumbullimin kur haset një etiketë specifike.",
    	["am"] = "የውሂብ ነጥቦችን ዋጋዎች ድምር ያሰላል። የተወሰነ መለያ ሲገኝ ድምሩን እንደገና ማስጀመር ይቻላል።",
    	["hy"] = "Հաշվում է տվյալակետերի արժեքների կուտակային գումարը։ Ընտրովի կերպով զրոյացնում է կուտակումը, երբ հանդիպում է նշված պիտակ։",
    	["az"] = "Məlumat nöqtələrinin qiymətlərinin yığılan cəmini hesablayır. İstəyə görə müəyyən etiketə rast gəldikdə yığmanı sıfırlayır.",
    	["bn"] = "ডেটা পয়েন্টের মানগুলোর ক্রমযোজিত যোগফল গণনা করে। নির্দিষ্ট কোনো লেবেল পাওয়া গেলে ঐচ্ছিকভাবে সঞ্চয়ন রিসেট করা যায়।",
    	["eu"] = "Datu-puntuen balioen batura metatua kalkulatzen du. Aukeran, metaketa berrezartzen du etiketa zehatz bat aurkitzen denean.",
    	["be"] = "Вылічвае назапашвальную суму значэнняў кропак даных. Пры неабходнасці скідае назапашванне пры сустрэчы з пэўным ярлыком.",
    	["bg"] = "Изчислява кумулативния сбор на стойностите на точките от данни. По избор нулира натрупването при срещане на определен етикет.",
    	["my"] = "ဒေတာမှတ်တန်ဖိုးများ၏ စုစုပေါင်းတိုးလာမှုကို တွက်ချက်သည်။ သတ်မှတ်ထားသော အညွှန်းတစ်ခုကို တွေ့ရှိသည့်အခါ စုဆောင်းမှုကို ပြန်လည်သတ်မှတ်နိုင်သည်။",
    	["ca"] = "Calcula la suma acumulada dels valors dels punts de dades. Opcionalment, reinicia l’acumulació quan es troba una etiqueta específica.",
    	["zh-Hans"] = "计算数据点值的累计总和。可选择在遇到特定标签时重置累计值。",
    	["zh-Hant"] = "計算資料點值的累計總和。遇到特定標籤時，也可以重設累計。",
    	["hr"] = "Izračunava kumulativni zbroj vrijednosti podatkovnih točaka. Po želji ponovno pokreće akumulaciju kada naiđe na određenu oznaku.",
    	["cs"] = "Vypočítá kumulativní součet hodnot datových bodů. Volitelně resetuje kumulaci při nalezení konkrétního štítku.",
    	["da"] = "Beregner den kumulative sum af datapunkternes værdier. Akkumuleringen kan valgfrit nulstilles, når en bestemt etiket registreres.",
    	["nl"] = "Berekent de cumulatieve som van de waarden van gegevenspunten. Kan de accumulatie optioneel resetten wanneer een specifiek label wordt aangetroffen.",
    	["et"] = "Arvutab andmepunktide väärtuste kumulatiivse summa. Soovi korral lähtestab kogumise konkreetse sildi leidmisel.",
    	["fil"] = "Kinakalkula ang pinagsama-samang kabuuan ng mga halaga ng data point. Maaari ring i-reset ang akumulasyon kapag may natukoy na label.",
    	["fi"] = "Laskee datapisteiden arvojen kumulatiivisen summan. Kertymän voi halutessaan nollata, kun tietty selite kohdataan.",
    	["fr"] = "Calcule la somme cumulée des valeurs des points de données. Peut éventuellement réinitialiser le cumul lorsqu’un libellé spécifique est rencontré.",
    	["gl"] = "Calcula a suma acumulada dos valores dos puntos de datos. Opcionalmente, restablece a acumulación cando se atopa unha etiqueta específica.",
    	["ka"] = "ითვლის მონაცემთა წერტილების მნიშვნელობების კუმულაციურ ჯამს. სურვილისამებრ, აკუმულირებას აღადგენს კონკრეტული იარლიყის აღმოჩენისას.",
    	["de"] = "Berechnet die kumulative Summe der Datenpunktwerte. Optional wird die Aufsummierung zurückgesetzt, wenn ein bestimmtes Label gefunden wird.",
    	["el"] = "Υπολογίζει το σωρευτικό άθροισμα των τιμών των σημείων δεδομένων. Προαιρετικά, μηδενίζει τη συσσώρευση όταν συναντάται συγκεκριμένη ετικέτα.",
    	["gu"] = "ડેટા પોઇન્ટના મૂલ્યોના સંચિત સરવાળાની ગણતરી કરે છે. વૈકલ્પિક રીતે, ચોક્કસ લેબલ મળે ત્યારે સંચય ફરીથી સેટ કરે છે.",
    	["hi"] = "डेटा पॉइंट के मानों के संचयी योग की गणना करता है। किसी विशिष्ट लेबल के मिलने पर संचय को वैकल्पिक रूप से रीसेट किया जा सकता है।",
    	["hu"] = "Kiszámítja az adatpontok értékeinek halmozott összegét. Opcionálisan újraindítja a halmozást egy adott címke megjelenésekor.",
    	["is"] = "Reiknar út uppsafnaða summu gilda gagnapunkta. Hægt er að endurstilla söfnunina þegar tiltekið merki finnst.",
    	["id"] = "Menghitung jumlah kumulatif nilai titik data. Secara opsional, akumulasi dapat diatur ulang saat label tertentu ditemukan.",
    	["it"] = "Calcola la somma cumulativa dei valori dei punti dati. Facoltativamente, reimposta l'accumulo quando viene rilevata un'etichetta specifica.",
    	["ja"] = "データポイントの値の累積合計を計算します。特定のラベルが見つかったときに累積をリセットすることもできます。",
    	["kn"] = "ಡೇಟಾ ಬಿಂದುಗಳ ಮೌಲ್ಯಗಳ ಸಂಚಿತ ಮೊತ್ತವನ್ನು ಲೆಕ್ಕಹಾಕುತ್ತದೆ. ನಿರ್ದಿಷ್ಟ ಲೇಬಲ್ ಎದುರಾದಾಗ ಸಂಚಯವನ್ನು ಐಚ್ಛಿಕವಾಗಿ ಮರುಹೊಂದಿಸಬಹುದು.",
    	["kk"] = "Дерек нүктелері мәндерінің жинақталған қосындысын есептейді. Белгілі бір жапсырма кездескенде жинақтауды қалпына келтіруге болады.",
    	["km"] = "គណនាផលបូកបង្គរនៃតម្លៃចំណុចទិន្នន័យ។ អាចកំណត់ឡើងវិញនូវការបង្គរ នៅពេលជួបស្លាកជាក់លាក់មួយ។",
    	["ko"] = "데이터 포인트 값의 누적 합계를 계산합니다. 특정 라벨이 발견되면 누적을 재설정하도록 선택할 수 있습니다.",
    	["ky"] = "Маалымат чекиттеринин маанилеринин жыйынды суммасын эсептейт. Белгилүү бир энбелги кездешкенде топтоону каалоого жараша баштапкы абалга келтирет.",
    	["lo"] = "ຄຳນວນຜົນບວກສະສົມຂອງຄ່າຈຸດຂໍ້ມູນ. ສາມາດຣີເຊັດການສະສົມເມື່ອພົບປ້າຍສະເພາະ.",
    	["lv"] = "Aprēķina datu punktu vērtību kumulatīvo summu. Pēc izvēles atiestata uzkrāšanu, kad tiek konstatēta konkrēta etiķete.",
    	["lt"] = "Apskaičiuoja kaupiamąją duomenų taškų reikšmių sumą. Pasirinktinai iš naujo pradeda kaupti, kai aptinkama konkreti etiketė.",
    	["mk"] = "Ја пресметува кумулативната сума на вредностите на точките на податоци. Изборно го ресетира собирањето кога ќе се сретне одредена ознака.",
    	["ms"] = "Mengira jumlah terkumpul nilai titik data. Secara pilihan, menetapkan semula pengumpulan apabila label tertentu ditemui.",
    	["ml"] = "ഡാറ്റാ പോയിന്റ് മൂല്യങ്ങളുടെ സഞ്ചിത തുക കണക്കാക്കുന്നു. ഒരു നിർദ്ദിഷ്ട ലേബൽ കണ്ടാൽ സഞ്ചയം ഐച്ഛികമായി റീസെറ്റ് ചെയ്യാം.",
    	["mr"] = "डेटा बिंदूंच्या मूल्यांची संचयी बेरीज मोजते. विशिष्ट लेबल आढळल्यावर संचय वैकल्पिकरित्या रीसेट करते.",
    	["mn"] = "Өгөгдлийн цэгүүдийн утгын хуримтлагдсан нийлбэрийг тооцоолно. Тодорхой шошго таарахад хуримтлалыг сонголтоор тэглэж болно.",
    	["ne"] = "डेटा बिन्दुका मानहरूको सञ्चित योग गणना गर्छ। वैकल्पिक रूपमा, निर्दिष्ट लेबल भेटिँदा सञ्चय रिसेट गर्छ।",
    	["no"] = "Beregner den kumulative summen av datapunktverdier. Kan valgfritt tilbakestille akkumuleringen når en bestemt etikett registreres.",
    	["pl"] = "Oblicza skumulowaną sumę wartości punktów danych. Opcjonalnie resetuje sumowanie po napotkaniu określonej etykiety.",
    	["pt"] = "Calcula a soma cumulativa dos valores dos pontos de dados. Opcionalmente, reinicia a acumulação quando encontra um rótulo específico.",
    	["pa"] = "ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਦੇ ਮੁੱਲਾਂ ਦਾ ਸੰਚਿਤ ਜੋੜ ਗਿਣਦਾ ਹੈ। ਚਾਹੋ ਤਾਂ ਕਿਸੇ ਖਾਸ ਲੇਬਲ ਦੇ ਮਿਲਣ ’ਤੇ ਸੰਚੈ ਨੂੰ ਰੀਸੈੱਟ ਕਰ ਸਕਦਾ ਹੈ।",
    	["ro"] = "Calculează suma cumulativă a valorilor punctelor de date. Opțional, resetează acumularea când este întâlnită o anumită etichetă.",
    	["rm"] = "Calcula la summa cumulativa da las valurs dals puncts da datas. Opziunalmain reinitialisescha l’accumulaziun cura ch’ina etichetta specifica vegn chattada.",
    	["ru"] = "Вычисляет накопительную сумму значений точек данных. При необходимости сбрасывает накопление при обнаружении определённой метки.",
    	["sr"] = "Izračunava kumulativni zbir vrednosti tačaka podataka. Po želji resetuje akumulaciju kada se pronađe određena oznaka.",
    	["si"] = "දත්ත ලක්ෂ්‍ය අගයන්ගේ සමුච්චිත එකතුව ගණනය කරයි. නිශ්චිත ලේබලයක් හමු වූ විට සමුච්චය යළි පිහිටුවීමට විකල්පයක් ඇත.",
    	["sk"] = "Vypočíta kumulatívny súčet hodnôt údajových bodov. Voliteľne resetuje kumuláciu pri výskyte konkrétneho označenia.",
    	["sl"] = "Izračuna kumulativno vsoto vrednosti podatkovnih točk. Po želji ponastavi seštevanje, ko zazna določeno oznako.",
    	["es"] = "Calcula la suma acumulada de los valores de los puntos de datos. Opcionalmente, restablece la acumulación cuando se encuentra una etiqueta específica.",
    	["sw"] = "Hukokotoa jumla inayojikusanya ya thamani za nukta za data. Kwa hiari huweka mkusanyiko upya inapokutana na lebo maalum.",
    	["sv"] = "Beräknar den kumulativa summan av datapunkternas värden. Ackumuleringen kan valfritt återställas när en specifik etikett påträffas.",
    	["ta"] = "தரவுப் புள்ளி மதிப்புகளின் கூட்டுத்தொகையைக் கணக்கிடுகிறது. குறிப்பிட்ட லேபிள் காணப்படும்போது கூட்டலை விருப்பமாக மீட்டமைக்கலாம்.",
    	["te"] = "డేటా పాయింట్ విలువల సంచిత మొత్తాన్ని లెక్కిస్తుంది. నిర్దిష్ట లేబుల్ కనిపించినప్పుడు సంచయాన్ని ఐచ్ఛికంగా రీసెట్ చేస్తుంది.",
    	["th"] = "คำนวณผลรวมสะสมของค่าจุดข้อมูล สามารถรีเซ็ตการสะสมได้เมื่อพบป้ายกำกับที่ระบุ",
    	["tr"] = "Veri noktası değerlerinin birikimli toplamını hesaplar. Belirli bir etiketle karşılaşıldığında birikimi isteğe bağlı olarak sıfırlar.",
    	["uk"] = "Обчислює накопичувальну суму значень точок даних. За потреби скидає накопичення, коли зустрічається певна мітка.",
    	["vi"] = "Tính tổng tích lũy của các giá trị điểm dữ liệu. Có thể đặt lại phép cộng dồn khi gặp một nhãn cụ thể.",
    },
    config = {
        checkbox {
            id = "enable_reset",
            name = "_reset_on_label_match",
            default = false,
        },
        text {
            id = "reset_label",
            name = "_reset_label",
            default = "",
        },
        checkbox {
            id = "exact_match",
            name = "_match_exactly",
            default = false,
        },
        checkbox {
            id = "case_sensitive",
            name = "_case_sensitive",
            default = true,
        },
    },

    -- Generator function
    generator = function(source, config)
        local enable_reset = config.enable_reset
        local reset_label = config.reset_label
        local exact_match = config.exact_match
        local case_sensitive = config.case_sensitive

        -- Helper function to check if a label matches
        local function label_matches(dp_label)
            if not enable_reset then
                return false
            end

            local label_to_check = dp_label or ""
            local pattern = reset_label

            -- Apply case insensitivity if needed
            if not case_sensitive then
                label_to_check = label_to_check:lower()
                pattern = pattern:lower()
            end

            -- Check match type
            if exact_match then
                return label_to_check == pattern
            else
                -- search for pattern from the start, pattern matching disabled
                return label_to_check:find(pattern, 1, true) ~= nil
            end
        end

        -- Drain all data points from source (returns in reverse chronological order)
        local all_points = source.dpall()

        -- Calculate cumulative values by iterating in reverse (chronologically)
        -- Mutate data points in place
        local cumulative_sum = 0
        for i = #all_points, 1, -1 do
            local data_point = all_points[i]

            -- Check if we should reset
            if label_matches(data_point.label) then
                cumulative_sum = 0
            end

            -- Add current value to cumulative sum
            cumulative_sum = cumulative_sum + data_point.value

            -- Update the data point's value in place
            data_point.value = cumulative_sum
        end

        -- all_points is still in reverse chronological order, return iterator
        local index = 0
        return function()
            index = index + 1
            return all_points[index]
        end
    end,
}
