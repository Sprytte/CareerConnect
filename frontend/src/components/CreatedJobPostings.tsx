import "../styles/CreatedJobPostings.css";
import SiteHeader from "./SiteHeader";
import SiteFooter from "./SiteFooter";
import { useSession } from "../auth/session";

type JobPosting = {
  id: number;
  title: string;
  company: string;
  location: string;
  category: string;
  description: string;
  createdAt: string;
};

const mockJobPostings: JobPosting[] = [
  {
    id: 1,
    title: "Software Developer Intern",
    company: "CareerConnect",
    location: "Montreal, QC",
    category: "Software Engineering",
    description:
      "Help build and improve web-based tools while working with a collaborative development team.",
    createdAt: "2026-10-08",
  },
  {
    id: 2,
    title: "Junior Embedded Systems Developer",
    company: "CareerConnect",
    location: "Montreal, QC",
    category: "Computer Engineering",
    description:
      "Work on embedded software development, testing, and integration for connected systems.",
    createdAt: "2026-10-06",
  },
];

export default function CreatedJobPostings() {
  const session = useSession();

  if (session.status === "loading") {
    return (
      <div className="job-postings-page">
        <SiteHeader />

        <main className="job-postings-main">
          <p className="job-postings-message">Loading account...</p>
        </main>

        <SiteFooter />
      </div>
    );
  }

  if (session.status === "signed-out") {
    return (
      <div className="job-postings-page">
        <SiteHeader />

        <main className="job-postings-main">
          <section className="job-postings-empty">
            <h1>Your Job Postings</h1>
            <p>Please sign in to view your created job postings.</p>
          </section>
        </main>

        <SiteFooter />
      </div>
    );
  }

  if (session.status === "unavailable") {
    return (
      <div className="job-postings-page">
        <SiteHeader />

        <main className="job-postings-main">
          <section className="job-postings-empty">
            <h1>Your Job Postings</h1>
            <p>Account information is currently unavailable.</p>
          </section>
        </main>

        <SiteFooter />
      </div>
    );
  }

  const jobPostings = mockJobPostings;

  return (
    <div className="job-postings-page">
      <SiteHeader />

      <main className="job-postings-main">
        <section className="job-postings-heading">
          <span className="job-postings-kicker">RECRUITER DASHBOARD</span>

          <h1>Your Job Postings</h1>

          <p>
            View the job opportunities you have created on CareerConnect.
          </p>
        </section>

        {jobPostings.length === 0 ? (
          <section className="job-postings-empty">
            <h2>No job postings yet</h2>
            <p>Jobs you create will appear here.</p>
          </section>
        ) : (
          <section className="job-postings-grid">
            {jobPostings.map((job) => (
              <article className="job-posting-card" key={job.id}>
                <div className="job-posting-top">
                  <div>
                    <span className="job-posting-category">
                      {job.category}
                    </span>

                    <h2>{job.title}</h2>
                  </div>

                  <span className="job-posting-id">
                    #{job.id}
                  </span>
                </div>

                <div className="job-posting-details">
                  <span>{job.company}</span>
                  <span className="job-posting-separator">•</span>
                  <span>{job.location}</span>
                </div>

                <p className="job-posting-description">
                  {job.description}
                </p>

                <div className="job-posting-footer">
                  <span>
                    Posted{" "}
                    {new Date(job.createdAt).toLocaleDateString()}
                  </span>
                </div>
              </article>
            ))}
          </section>
        )}
      </main>

      <SiteFooter />
    </div>
  );
}