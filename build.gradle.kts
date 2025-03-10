version = 1

android {
    defaultConfig {
        namespace = "com.keyiflerolsun.ddizi"
    }
}

cloudstream {
    language = "tr"
    description = "DDizi.im üzerinden dizi izleyebilmenizi sağlar"
    authors = listOf("keyiflerolsun")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "TvSeries",
    )
} 