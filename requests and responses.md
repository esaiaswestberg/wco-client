# Get Iframe URL (Step 1)
Find the `iframe` element in the episode page HTML source. The `src` attribute is the target URL.

## Example URL
`https://embed.wcostream.com/inc/embed/index.php?file=Cartoons%2FFamily%20Guy%2Ffamily.guy.s23e10.1080p.web.h264-successfulcrab.flv&fullhd=1&pid=943999&h=d5e62196d1582bd48b8e7924016b260a&t=1769089837&embed=neptun`

## CURL
```bash
curl -L "https://embed.wcostream.com/inc/embed/index.php?file=Cartoons%2FFamily%20Guy%2Ffamily.guy.s23e10.1080p.web.h264-successfulcrab.flv&fullhd=1&pid=943999&h=d5e62196d1582bd48b8e7924016b260a&t=1769089837&embed=neptun" \
  -H "Referer: https://www.wcostream.tv/" \
  -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
```

## Response (HTML Snippet)
Search the response for `$.getJSON` to find the API URL for the next step.
```javascript
<script>
    // ...
    $.getJSON("/inc/embed/getvidlink.php?v=neptun/Cartoons/Family%20Guy/family.guy.s23e10.1080p.web.h264-successfulcrab.mp4&embed=neptun&fullhd=1", function(response){
        vsd     = response.enc;
        vhd     = response.hd;
        vfhd    = response.fhd;
        cdn     = response.cdn;
        server  = response.server;
        // ...
    });
</script>
```

# Get Video JSON Tokens (Step 2)

## CURL
```bash
curl "https://embed.wcostream.com/inc/embed/getvidlink.php?v=Family%20Guy%20Season%2020/Family.Guy.S20E18.Girlfriend.Eh.1080p.mp4&embed=ndisk&hd=1" \
  -H "X-Requested-With: XMLHttpRequest" \
  -H "Referer: https://embed.wcostream.com/" \
  -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
```

## Response
```json
{
  "enc": "d4RmjWURmG3OausnxCJLFLy2Gx2M6oo0-p1s9KqHqv9ms9hgiSN2yif3BX614P_XE5SbMj9smqE8kotWWWLwHAHT_i4ItzIvKRTS7yzMlqitfCT2vUszoiYHgf8lW1nw3jXURyxFw6OdAjHppZIvNXrd0nqoV31vkbjLQhsh9oCzifi94C_GoFCXMPO6v4cQuKvMS3Virv4FY-iDHkLOR0pKisQo_vJ4lVkBv3k0omrPEujvFFCgBzLS_4MO7ek_pFveI5IwzLO6hwYZ0qQ6aGcp2C5KXQun5BUvt_BLcgIKSTyZhL18JI7xv1A2dymEzH_ILLXlcRI9tuqOgF9P7X1hz_sncB2vN3qvg9da1IIjJE6LaTqVQSNckz1-qmrg",
  "server": "https://ndisk.wcostream.com",
  "cdn": "https://ndisk.wcostream.com",
  "hd": "d4RmjWURmG3OausnxCJLFLy2Gx2M6oo0-p1s9KqHqv9ms9hgiSN2yif3BX614P_XE5SbMj9smqE8kotWWWLwHHuyv88UtGxtLgnqVx1X3zpdgJ41n4Cltb6vxhtcrMOJWBmphQSjxRx2dNYYmb9y07oTZwdalpcqObsdaH3ifTdoZIfryJfSHhIOYpctgqzI8FJprI9DlM0M_PqJ-4WhQuSwDDjrbVm1AxCnO0L4ihV5bcfcMINahb39MsZhmdEkFATKyGLFs6v_WNTFUXVeGW3rp8Gr_Q1OqF547Um5BGkrMk-C6UYicRzVeIQcCgIz7ejYEq4FEUf1E-sC2wHYJ4k2GeVKzDrgjRe2StwaKUFdS23Vb8QA2naKmPxw0Mzl",
  "fhd": "",
  "sub": ""
}
```

# Select Quality & Get Redirect URL (Step 3)
The JSON response from Step 2 contains tokens for different qualities. Construct the URL using the `server` base URL and the corresponding token:

*   **SD (Default)**: Use key `enc` $\rightarrow$ `${server}/getvid?evid=${enc}&json`
*   **HD (720p)**: Use key `hd` (if present) $\rightarrow$ `${server}/getvid?evid=${hd}&json`
*   **FHD (1080p)**: Use key `fhd` (if present) $\rightarrow$ `${server}/getvid?evid=${fhd}&json`

## CURL (Example for SD)
```bash
curl "https://ndisk.wcostream.com/getvid?evid=d4RmjWURmG3OausnxCJLFLy2Gx2M6oo0-p1s9KqHqv9ms9hgiSN2yif3BX614P_XE5SbMj9smqE8kotWWWLwHAHT_i4ItzIvKRTS7yzMlqitfCT2vUszoiYHgf8lW1nw3jXURyxFw6OdAjHppZIvNXrd0nqoV31vkbjLQhsh9oCzifi94C_GoFCXMPO6v4cQuKvMS3Virv4FY-iDHkLOR0pKisQo_vJ4lVkBv3k0omrPEujvFFCgBzLS_4MO7ek_pFveI5IwzLO6hwYZ0qQ6aGcp2C5KXQun5BUvt_BLcgIKSTyZhL18JI7xv1A2dymEzH_ILLXlcRI9tuqOgF9P7X1hz_sncB2vN3qvg9da1IIjJE6LaTqVQSNckz1-qmrg&json" \
  -H "X-Requested-With: XMLHttpRequest" \
  -H "Referer: https://embed.wcostream.com/" \
  -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
```

## Response
```json
"https:\/\/t01.wcostream.com\/getvid?evid=Kks11MUx7ypWFe2_QTbI87b0NPWk8K0kfm4PfMc683KGJB-KRtio83nrsTeqzL-FaIYxKVUluPXcy8udPgmSgOOTRjDsXCiisxNXDSr0FuElIz_dY8s4xlm6MABvd62_RzZbQqUwrG4Kb8s0-kGnZtLCtVS01su2Im3gPxjHP-Q1RlJmQezv7-DUbHr6PSygFkrOPsOUQKtH1BWivTriL95kO55iBT6xaL7mXLIyPHt1Lpi4LS_wxOJLbskTOwsg7qRsXBC7xWp6dh_aaLLWywnOqh-x-yvHfEpfgvze31p9DNNUPouUcMguIvk2n9a98d1XbQvNKuFjQcfSA0FyaMN_5UsXyZ7fbjnJs-VOMWzd4UIS6Lc4ZmC-mFJmV9VGOVoH1Pq7k1Sz1J7hVu5ZEA&json"
```

# Download Video Stream (Step 4)
Use the URL returned from Step 3.
**Important:**
1. Remove the `&json` suffix (if present).
2. **Remove** the `X-Requested-With` header (browsers don't send it for media requests).
3. Add `Accept` and `Range` headers to mimic a video player request.

## CURL
```bash
curl "https://t01.wcostream.com/getvid?evid=Kks11MUx7ypWFe2_QTbI87b0NPWk8K0kfm4PfMc683KGJB-KRtio83nrsTeqzL-FaIYxKVUluPXcy8udPgmSgOOTRjDsXCiisxNXDSr0FuElIz_dY8s4xlm6MABvd62_RzZbQqUwrG4Kb8s0-kGnZtLCtVS01su2Im3gPxjHP-Q1RlJmQezv7-DUbHr6PSygFkrOPsOUQKtH1BWivTriL95kO55iBT6xaL7mXLIyPHt1Lpi4LS_wxOJLbskTOwsg7qRsXBC7xWp6dh_aaLLWywnOqh-x-yvHfEpfgvze31p9DNNUPouUcMguIvk2n9a98d1XbQvNKuFjQcfSA0FyaMN_5UsXyZ7fbjnJs-VOMWzd4UIS6Lc4ZmC-mFJmV9VGOVoH1Pq7k1Sz1J7hVu5ZEA" \
  -H "Referer: https://embed.wcostream.com/" \
  -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36" \
  -H "Accept: video/mp4,video/*;q=0.9,*/*;q=0.8" \
  -H "Range: bytes=0-" \
  -o episode.mp4
```

## Response
```
HTTP/1.1 206 Partial Content
Server: nginx
Date: Thu, 22 Jan 2026 10:47:06 GMT
Content-Type: video/mp4
Content-Length: 78757020
Last-Modified: Fri, 13 May 2022 19:57:09 GMT
Connection: close
ETag: "627eb815-4b1bc9c"
Content-Range: bytes 0-78757019/78757020
```