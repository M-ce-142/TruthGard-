package com.example.data.remote

import com.example.data.model.NewsArticle
import com.example.data.model.Verdict

class NewsApiService {

    fun getSampleNewsList(): List<NewsArticle> {
        return listOf(
            NewsArticle(
                id = "n1",
                title = "Ministry Warns Against Fake 'Digital Arrest' Calls Impersonating CBI & Customs",
                summary = "Cyber Coordination Centre issues urgent alert warning citizens that law enforcement never places video calls for arrests or monetary settlements.",
                category = "Politics",
                source = "PIB Fact Check",
                publishedTime = "10 mins ago",
                url = "https://pib.gov.in",
                verificationStatus = Verdict.REAL,
                verifiedCount = 3840
            ),
            NewsArticle(
                id = "n2",
                title = "Viral WhatsApp Message Claiming Free 3-Month Mobile Recharge Scheme is FAKE",
                summary = "A fake claim promising free mobile recharge under government youth scheme is circulating on social media with phishing links.",
                category = "Technology",
                source = "Factcheck.org",
                publishedTime = "25 mins ago",
                url = "https://factcheck.org",
                verificationStatus = Verdict.FAKE,
                verifiedCount = 5120
            ),
            NewsArticle(
                id = "n3",
                title = "RBI Clarifies No New ₹500 Banknotes Being Withdrawn or Discontinued",
                summary = "Reserve Bank of India debunks social media rumor claiming old series 500 notes will become invalid at the end of the month.",
                category = "Business",
                source = "Reuters",
                publishedTime = "1 hour ago",
                url = "https://reuters.com",
                verificationStatus = Verdict.REAL,
                verifiedCount = 2950
            ),
            NewsArticle(
                id = "n4",
                title = "Fake Part-Time Job Offer Scam Circulating via Telegram & WhatsApp",
                summary = "Scammers target students and job seekers with promises of ₹5,000 daily for liking YouTube videos. Over ₹10 Cr frozen in bank accounts.",
                category = "Education",
                source = "Cyber Crime Department",
                publishedTime = "2 hours ago",
                url = "https://cybercrime.gov.in",
                verificationStatus = Verdict.FAKE,
                verifiedCount = 6100
            ),
            NewsArticle(
                id = "n5",
                title = "ISRO Successfully Tests New Next-Gen Engine for Deep Space Missions",
                summary = "Indian Space Research Organisation successfully completes hot testing of cryogenic stage engine for future Gaganyaan and lunar launches.",
                category = "Science",
                source = "ISRO Official Press",
                publishedTime = "3 hours ago",
                url = "https://isro.gov.in",
                verificationStatus = Verdict.REAL,
                verifiedCount = 4210
            ),
            NewsArticle(
                id = "n6",
                title = "AI Medical Scan Tool Approved for Faster Early Cancer Detection in Hospitals",
                summary = "Health regulators approve AI assistant tool to help radiologists detect early-stage lung anomalies with 99.2% accuracy.",
                category = "Health",
                source = "Medical Times",
                publishedTime = "4 hours ago",
                url = "https://medicaltimes.org",
                verificationStatus = Verdict.REAL,
                verifiedCount = 1890
            ),
            NewsArticle(
                id = "n7",
                title = "Claimed Deepfake Video of Celebrity Promoting Crypto Investment Proven Manipulated",
                summary = "AI forensic tools confirm celebrity voice and lip-sync in viral Instagram Reel were generated using neural speech cloning.",
                category = "Entertainment",
                source = "AltNews",
                publishedTime = "5 hours ago",
                url = "https://altnews.in",
                verificationStatus = Verdict.FAKE,
                verifiedCount = 3100
            ),
            NewsArticle(
                id = "n8",
                title = "National Cricket Team Announces Squad for Upcoming World Championship",
                summary = "Board of Control for Cricket announces 15-player squad featuring two new young bowlers following strong domestic performance.",
                category = "Sports",
                source = "Sports Wire",
                publishedTime = "6 hours ago",
                url = "https://sportswire.com",
                verificationStatus = Verdict.REAL,
                verifiedCount = 2740
            )
        )
    }
}
